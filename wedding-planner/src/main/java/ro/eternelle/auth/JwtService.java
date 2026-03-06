package ro.eternelle.auth;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import ro.eternelle.user.User;

import java.time.Duration;
import java.util.Set;

@ApplicationScoped
public class JwtService {

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    private static final Duration TOKEN_EXPIRY = Duration.ofHours(24);

    public String generateToken(User user) {
        return Jwt.issuer(issuer)
                .subject(user.id.toString())
                .groups(Set.of(user.role.name()))
                .claim("email", user.email)
                .claim("role", user.role.name())
                .expiresIn(TOKEN_EXPIRY)
                .sign();
    }
}
