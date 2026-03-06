package ro.eternelle.vendor;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@ApplicationScoped
public class ProfileViewRepository implements PanacheRepositoryBase<ProfileView, UUID> {

    /** Total views for a vendor. */
    public long countByVendor(UUID vendorId) {
        return count("vendor.id", vendorId);
    }

    /** Views since a given instant. */
    public long countByVendorSince(UUID vendorId, Instant since) {
        return count("vendor.id = ?1 AND viewedAt >= ?2", vendorId, since);
    }

    /** Views within a time window. */
    public long countByVendorBetween(UUID vendorId, Instant from, Instant to) {
        return count("vendor.id = ?1 AND viewedAt >= ?2 AND viewedAt < ?3", vendorId, from, to);
    }

    /**
     * Dedup check: has this viewer (or the same IP) already been counted today for this vendor?
     * For authenticated users, dedup on viewer_id + vendor + day.
     * For anonymous, dedup on ip_hash + vendor + day.
     */
    public boolean existsToday(UUID vendorId, UUID viewerId, String ipHash) {
        Instant dayStart = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);

        if (viewerId != null) {
            return count("vendor.id = ?1 AND viewer.id = ?2 AND viewedAt >= ?3",
                    vendorId, viewerId, dayStart) > 0;
        }
        if (ipHash != null) {
            return count("vendor.id = ?1 AND viewer IS NULL AND ipHash = ?2 AND viewedAt >= ?3",
                    vendorId, ipHash, dayStart) > 0;
        }
        return false;
    }
}
