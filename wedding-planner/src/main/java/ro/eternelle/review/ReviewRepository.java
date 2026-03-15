package ro.eternelle.review;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ReviewRepository implements PanacheRepositoryBase<Review, UUID> {

    /** Returns only APPROVED reviews — used for public vendor page and rating calculation. */
    public List<Review> findByVendor(UUID vendorId) {
        return find("vendor.id = ?1 AND status = ?2 ORDER BY createdAt DESC", vendorId, "APPROVED").list();
    }

    /** Returns ALL reviews for a vendor regardless of status — used for the vendor's own reviews page. */
    public List<Review> findAllByVendor(UUID vendorId) {
        return find("vendor.id = ?1 ORDER BY createdAt DESC", vendorId).list();
    }

    public boolean existsByBooking(UUID bookingId) {
        return count("booking.id", bookingId) > 0;
    }

    /** Returns the most recent review date for a vendor, or empty if none. */
    public Optional<Review> findLatestByVendor(UUID vendorId) {
        return find("vendor.id = ?1 ORDER BY createdAt DESC", vendorId).firstResultOptional();
    }
}
