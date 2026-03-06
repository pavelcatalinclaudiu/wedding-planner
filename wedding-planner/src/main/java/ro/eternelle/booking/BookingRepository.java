package ro.eternelle.booking;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class BookingRepository implements PanacheRepositoryBase<Booking, UUID> {

    public Optional<Booking> findByLead(UUID leadId) {
        return find("lead.id", leadId).firstResultOptional();
    }

    public List<Booking> findByCouple(UUID coupleId) {
        return find("lead.couple.id", coupleId).list();
    }

    public List<Booking> findByVendor(UUID vendorId) {
        return find("lead.vendor.id", vendorId).list();
    }

    /** Sum of totalPrice for bookings created since the given instant for a vendor. */
    public BigDecimal sumRevenueForVendorSince(UUID vendorId, Instant since) {
        BigDecimal result = (BigDecimal) getEntityManager().createQuery(
                "SELECT COALESCE(SUM(b.totalPrice), 0) FROM Booking b " +
                "JOIN b.lead l WHERE l.vendor.id = :vendorId AND b.createdAt >= :since")
                .setParameter("vendorId", vendorId)
                .setParameter("since", since)
                .getSingleResult();
        return result != null ? result : BigDecimal.ZERO;
    }

    /** Sum of totalPrice for bookings within a time window for a vendor. */
    public BigDecimal sumRevenueForVendorBetween(UUID vendorId, Instant from, Instant to) {
        BigDecimal result = (BigDecimal) getEntityManager().createQuery(
                "SELECT COALESCE(SUM(b.totalPrice), 0) FROM Booking b " +
                "JOIN b.lead l WHERE l.vendor.id = :vendorId AND b.createdAt >= :from AND b.createdAt < :to")
                .setParameter("vendorId", vendorId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
        return result != null ? result : BigDecimal.ZERO;
    }

    /** Count of bookings for a vendor since the given instant. */
    public long countConfirmedForVendorSince(UUID vendorId, Instant since) {
        return (long) getEntityManager().createQuery(
                "SELECT COUNT(b) FROM Booking b " +
                "JOIN b.lead l WHERE l.vendor.id = :vendorId AND b.createdAt >= :since")
                .setParameter("vendorId", vendorId)
                .setParameter("since", since)
                .getSingleResult();
    }

    /**
     * Revenue grouped by year-month string ("YYYY-MM") for the last N months.
     * Returns rows of Object[]{yearMonth, sumRevenue, bookingCount}.
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> revenueGroupedByMonth(UUID vendorId, Instant since) {
        return getEntityManager().createQuery(
                "SELECT FUNCTION('DATE_FORMAT', b.createdAt, '%Y-%m'), " +
                "       COALESCE(SUM(b.totalPrice), 0), " +
                "       COUNT(b) " +
                "FROM Booking b JOIN b.lead l " +
                "WHERE l.vendor.id = :vendorId AND b.createdAt >= :since " +
                "GROUP BY FUNCTION('DATE_FORMAT', b.createdAt, '%Y-%m') " +
                "ORDER BY 1")
                .setParameter("vendorId", vendorId)
                .setParameter("since", since)
                .getResultList();
    }
}
