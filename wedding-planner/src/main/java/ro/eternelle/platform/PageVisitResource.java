package ro.eternelle.platform;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Path("/api/public")
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
public class PageVisitResource {

    @Inject
    PageVisitRepository pageVisitRepository;

    /**
     * Called by the landing page on mount. Records one visit per IP per day.
     * No authentication required — completely open endpoint.
     */
    @POST
    @Path("/page-visit")
    @Transactional
    public Response trackPageVisit(@Context HttpHeaders httpHeaders) {
        try {
            String rawIp = httpHeaders.getHeaderString("X-Forwarded-For");
            if (rawIp == null) rawIp = httpHeaders.getHeaderString("X-Real-IP");
            // Take only first IP if X-Forwarded-For has a chain
            if (rawIp != null && rawIp.contains(",")) rawIp = rawIp.split(",")[0].trim();

            String ipHash = rawIp != null ? sha256Short(rawIp) : null;

            // Dedup: only record once per IP per calendar day
            if (!pageVisitRepository.existsToday(ipHash)) {
                PageVisit visit = new PageVisit();
                visit.ipHash = ipHash;
                pageVisitRepository.persist(visit);
            }
        } catch (Exception ignored) {
            // Never surface tracking errors to the visitor
        }
        return Response.noContent().build();
    }

    /** First 16 hex chars of SHA-256 — enough for dedup, not reversible back to IP. */
    private static String sha256Short(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(16);
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", hash[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
