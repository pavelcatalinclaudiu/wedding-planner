package ro.eternelle.videocall;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class VideoCallRepository implements PanacheRepositoryBase<VideoCall, UUID> {

    public List<VideoCall> findByLead(UUID leadId) {
        return find("lead.id", leadId).list();
    }

    public List<VideoCall> findScheduledBetween(Instant from, Instant to) {
        return find("status = ?1 AND scheduledAt >= ?2 AND scheduledAt < ?3",
                VideoCallStatus.SCHEDULED, from, to).list();
    }

    public List<VideoCall> findByVendorUserId(UUID userId) {
        return find("lead.vendor.user.id = ?1 ORDER BY scheduledAt DESC", userId).list();
    }

    public List<VideoCall> findByCoupleUserId(UUID userId) {
        return find("lead.couple.user.id = ?1 ORDER BY scheduledAt DESC", userId).list();
    }

    /**
     * Fetches a VideoCall by id with lead, couple, and vendor eagerly initialized
     * so DTO serialization works outside the transaction.
     */
    public VideoCall findByIdFetched(UUID id) {
        return find(
            "SELECT v FROM VideoCall v " +
            "LEFT JOIN FETCH v.lead l " +
            "LEFT JOIN FETCH l.couple " +
            "LEFT JOIN FETCH l.vendor " +
            "WHERE v.id = ?1", id
        ).firstResult();
    }

    /**
     * Finds all SCHEDULED or IN_PROGRESS calls whose scheduled time is before
     * {@code cutoff} — i.e. the 30-minute window has already elapsed.
     * Used by the auto-complete scheduler.
     */
    public List<VideoCall> findExpiredActive(Instant cutoff) {
        return find("(status = ?1 OR status = ?2) AND scheduledAt < ?3",
                VideoCallStatus.SCHEDULED, VideoCallStatus.IN_PROGRESS, cutoff).list();
    }

    /** Next N scheduled (future) calls for a given vendor, ordered by scheduledAt ASC. */
    public List<VideoCall> findUpcomingByVendor(UUID vendorId, int limit) {
        return find("lead.vendor.id = ?1 AND status = ?2 AND scheduledAt > ?3 ORDER BY scheduledAt ASC",
                vendorId, VideoCallStatus.SCHEDULED, Instant.now())
                .page(0, limit)
                .list();
    }
}
