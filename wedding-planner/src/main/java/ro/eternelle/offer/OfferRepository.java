package ro.eternelle.offer;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import ro.eternelle.booking.BookingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class OfferRepository implements PanacheRepositoryBase<Offer, UUID> {

    public List<Offer> findByLead(UUID leadId) {
        return find("lead.id = ?1", Sort.by("createdAt").descending(), leadId).list();
    }

    public List<Offer> findByConversation(UUID conversationId) {
        return find("conversation.id = ?1", Sort.by("createdAt").descending(), conversationId).list();
    }

    public java.util.Optional<Offer> findPendingByLead(UUID leadId) {
        return find("lead.id = ?1 AND status = ?2", leadId, OfferStatus.PENDING)
                .firstResultOptional();
    }

    public BigDecimal sumAcceptedRevenueForVendorSince(UUID vendorId, Instant since) {
        BigDecimal result = (BigDecimal) getEntityManager().createQuery(
                "SELECT COALESCE(SUM(o.price), 0) FROM Offer o " +
                "WHERE o.vendor.id = :vendorId AND o.status = :status AND o.createdAt >= :since " +
                "AND NOT EXISTS (SELECT b FROM Booking b WHERE b.lead.id = o.lead.id AND b.status = :cancelled)")
                .setParameter("vendorId", vendorId)
                .setParameter("status", OfferStatus.ACCEPTED)
                .setParameter("since", since)
                .setParameter("cancelled", BookingStatus.CANCELLED)
                .getSingleResult();
        return result != null ? result : BigDecimal.ZERO;
    }

    public BigDecimal sumAcceptedRevenueForVendorBetween(UUID vendorId, Instant from, Instant to) {
        BigDecimal result = (BigDecimal) getEntityManager().createQuery(
                "SELECT COALESCE(SUM(o.price), 0) FROM Offer o " +
                "WHERE o.vendor.id = :vendorId AND o.status = :status AND o.createdAt >= :from AND o.createdAt < :to " +
                "AND NOT EXISTS (SELECT b FROM Booking b WHERE b.lead.id = o.lead.id AND b.status = :cancelled)")
                .setParameter("vendorId", vendorId)
                .setParameter("status", OfferStatus.ACCEPTED)
                .setParameter("from", from)
                .setParameter("to", to)
                .setParameter("cancelled", BookingStatus.CANCELLED)
                .getSingleResult();
        return result != null ? result : BigDecimal.ZERO;
    }

    public long countAcceptedForVendorSince(UUID vendorId, Instant since) {
        return (long) getEntityManager().createQuery(
                "SELECT COUNT(o) FROM Offer o " +
                "WHERE o.vendor.id = :vendorId AND o.status = :status AND o.createdAt >= :since " +
                "AND NOT EXISTS (SELECT b FROM Booking b WHERE b.lead.id = o.lead.id AND b.status = :cancelled)")
                .setParameter("vendorId", vendorId)
                .setParameter("status", OfferStatus.ACCEPTED)
                .setParameter("since", since)
                .setParameter("cancelled", BookingStatus.CANCELLED)
                .getSingleResult();
    }
}
