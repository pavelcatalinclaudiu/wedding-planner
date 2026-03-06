package ro.eternelle.lead;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class LeadRepository implements PanacheRepositoryBase<Lead, UUID> {

    @Inject
    EntityManager em;

    public List<Lead> findByVendor(UUID vendorId) {
        return find("vendor.id = ?1", Sort.by("createdAt").descending(), vendorId).list();
    }

    public List<Lead> findByVendorAndStatus(UUID vendorId, LeadStatus status) {
        return find("vendor.id = ?1 AND status = ?2", Sort.by("createdAt").descending(), vendorId, status).list();
    }

    public List<Lead> findByCouple(UUID coupleId) {
        return find("couple.id = ?1", Sort.by("createdAt").descending(), coupleId).list();
    }

    public long countByVendor(UUID vendorId) {
        return count("vendor.id", vendorId);
    }

    public long countByVendorSince(UUID vendorId, Instant since) {
        return count("vendor.id = ?1 AND createdAt >= ?2", vendorId, since);
    }

    public long countByVendorBetween(UUID vendorId, Instant from, Instant to) {
        return count("vendor.id = ?1 AND createdAt >= ?2 AND createdAt < ?3", vendorId, from, to);
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> countByVendorGroupedByMonth(UUID vendorId, Instant since) {
        return em.createNativeQuery(
                "SELECT DATE_FORMAT(created_at, '%Y-%m') AS month, COUNT(*) AS cnt " +
                "FROM leads WHERE vendor_id = :vendorId AND created_at >= :since " +
                "GROUP BY month ORDER BY month")
                .setParameter("vendorId", vendorId.toString())
                .setParameter("since", since)
                .getResultList();
    }

    public List<Lead> findRecentByVendor(UUID vendorId, int limit) {
        return find("vendor.id = ?1", Sort.by("createdAt").descending(), vendorId)
                .page(0, limit).list();
    }

    public long countNewByVendor(UUID vendorId) {
        return count("vendor.id = ?1 AND status = ?2", vendorId, LeadStatus.NEW);
    }

    /**
     * Returns [{total, timely}] — total leads in window and leads responded to within 24 h.
     * A lead is "timely" if it was accepted/declined within 24 hours of creation.
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> calculateResponseRate(UUID vendorId, Instant from, Instant to) {
        String sql =
                "SELECT COUNT(*) AS total, " +
                "SUM(CASE WHEN status != 'NEW' AND TIMESTAMPDIFF(HOUR, created_at, updated_at) <= 24 THEN 1 ELSE 0 END) AS timely " +
                "FROM leads WHERE vendor_id = :vendorId AND created_at >= :from AND created_at < :to";
        return em.createNativeQuery(sql)
                .setParameter("vendorId", vendorId.toString())
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }
}
