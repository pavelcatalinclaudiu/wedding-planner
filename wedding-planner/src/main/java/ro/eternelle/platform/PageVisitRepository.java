package ro.eternelle.platform;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@ApplicationScoped
public class PageVisitRepository implements PanacheRepositoryBase<PageVisit, UUID> {

    /** Returns true if this ip_hash has already been recorded today. */
    public boolean existsToday(String ipHash) {
        if (ipHash == null) return false;
        Instant dayStart = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        return count("ipHash = ?1 AND visitedAt >= ?2", ipHash, dayStart) > 0;
    }
}
