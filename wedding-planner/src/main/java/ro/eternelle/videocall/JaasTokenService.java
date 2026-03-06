package ro.eternelle.videocall;

import io.jsonwebtoken.Jwts;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class JaasTokenService {

    private static final Logger LOG = Logger.getLogger(JaasTokenService.class);

    @ConfigProperty(name = "jaas.app.id")
    String appId;

    @ConfigProperty(name = "jaas.kid")
    String kid;

    public String moderatorToken(String roomName, String displayName) {
        return buildToken(roomName, displayName, true);
    }

    public String participantToken(String roomName, String displayName) {
        return buildToken(roomName, displayName, false);
    }

    private String buildToken(String roomName, String displayName, boolean moderator) {
        try {
            PrivateKey key = loadPrivateKey();
            if (key == null) return null;

            Instant now = Instant.now();

            // JaaS conference-mapper looks up the public key using the kid header.
            // The kid MUST be in "appId/keyId" format — e.g. "vpaas-magic-cookie-xxx/AbCdEfGh".
            // If JAAS_KID was configured as just the bare key ID, prepend the appId.
            String fullKid = kid.contains("/") ? kid : appId + "/" + kid;

            String token = Jwts.builder()
                    .header().add("kid", fullKid).add("typ", "JWT").and()
                    .issuer("chat")
                    .subject(appId)
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(now.plusSeconds(7200)))
                    .notBefore(Date.from(now.minusSeconds(10)))
                    .claim("aud", "jitsi")
                    .claim("context", Map.of(
                            "user", Map.of(
                                    "id", UUID.randomUUID().toString(),
                                    "name", displayName,
                                    "email", "",
                                    "avatar", "",
                                    "moderator", String.valueOf(moderator)  // JaaS/Prosody requires string "true"/"false", not boolean
                            ),
                            "features", Map.of(
                                    "recording", "false",
                                    "livestreaming", "false",
                                    "outbound-call", "false"
                            )
                    ))
                    // Use the specific room name — never "*".
                    // JaaS's conference-mapper rejects wildcard tokens with 404
                    // and Jicofo blocks all audio/video unmute when the room can't
                    // be matched to a valid authenticated conference.
                    .claim("room", roomName)
                    .signWith(key)
                    .compact();

            LOG.infof("Generated JaaS token (decode at jwt.io): %s", token);
            return token;
        } catch (Exception e) {
            LOG.warn("Failed to generate JaaS token: " + e.getMessage());
            return null;
        }
    }

    private PrivateKey loadPrivateKey() {
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("META-INF/resources/jaas-private.pem")) {
            if (is == null) {
                LOG.warn("jaas-private.pem not found — JaaS tokens disabled");
                return null;
            }
            String pem = new String(is.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(pem);
            return KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(decoded));
        } catch (Exception e) {
            LOG.warn("Failed to load jaas-private.pem: " + e.getMessage());
            return null;
        }
    }
}
