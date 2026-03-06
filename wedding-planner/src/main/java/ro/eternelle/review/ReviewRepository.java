package ro.eternelle.review;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ReviewRepository implements PanacheRepositoryBase<Review, UUID> {

    public List<Review> findByVendor(UUID vendorId) {
        return find("vendor.id = ?1 AND isPublic = true ORDER BY createdAt DESC", vendorId).list();
    }

    public boolean existsByBooking(UUID bookingId) {
        return count("booking.id", bookingId) > 0;
    }

    /** Returns the most recent review date for a vendor, or empty if none. */
    public Optional<Review> findLatestByVendor(UUID vendorId) {
        return find("vendor.id = ?1 ORDER BY createdAt DESC", vendorId).firstResultOptional();
    }
}
