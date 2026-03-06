package ro.eternelle.review;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ro.eternelle.booking.Booking;
import ro.eternelle.booking.BookingRepository;
import ro.eternelle.couple.CoupleRepository;
import ro.eternelle.exception.BusinessException;
import ro.eternelle.vendor.VendorProfile;
import ro.eternelle.vendor.VendorRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class ReviewService {

    @Inject ReviewRepository reviewRepository;
    @Inject BookingRepository bookingRepository;
    @Inject CoupleRepository coupleRepository;
    @Inject VendorRepository vendorRepository;
    @Inject ro.eternelle.vendor.VendorAnalyticsService vendorAnalyticsService;

    public List<ReviewDTO> getByVendor(UUID vendorId) {
        return reviewRepository.findByVendor(vendorId).stream()
                .map(ReviewDTO::from)
                .collect(Collectors.toList());
    }

    public List<ReviewDTO> getByVendorUser(UUID userId) {
        VendorProfile vendor = vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Vendor profile not found"));
        return getByVendor(vendor.id);
    }

    @Transactional
    public ReviewDTO addReply(UUID reviewId, UUID vendorUserId, String reply) {
        Review review = reviewRepository.findById(reviewId);
        if (review == null) throw new BusinessException("Review not found");
        VendorProfile vendor = vendorRepository.findByUserId(vendorUserId)
                .orElseThrow(() -> new BusinessException("Vendor profile not found"));
        if (!review.vendor.id.equals(vendor.id)) throw new BusinessException("Not authorized");
        review.vendorReply = reply;
        return ReviewDTO.from(review);
    }

    @Transactional
    public ReviewDTO createReview(UUID bookingId, UUID coupleUserId, BigDecimal rating, String comment) {
        Booking booking = bookingRepository.findById(bookingId);
        if (booking == null) throw new BusinessException("Booking not found");

        if (reviewRepository.existsByBooking(bookingId)) {
            throw new BusinessException("Review already submitted for this booking");
        }

        var couple = coupleRepository.findByUserId(coupleUserId)
                .orElseThrow(() -> new BusinessException("Couple profile not found"));

        if (!booking.lead.couple.id.equals(couple.id)) {
            throw new BusinessException("Not authorized to review this booking");
        }

        Review review = new Review();
        review.booking = booking;
        review.couple = couple;
        review.vendor = booking.lead.vendor;
        review.rating = rating;
        review.comment = comment;
        reviewRepository.persist(review);

        recalculateVendorRating(booking.lead.vendor.id);
        vendorAnalyticsService.invalidateOverview(booking.lead.vendor.id);
        return ReviewDTO.from(review);
    }

    private void recalculateVendorRating(UUID vendorId) {
        List<Review> reviews = reviewRepository.findByVendor(vendorId);
        double avg = reviews.stream()
                .mapToDouble(r -> r.rating.doubleValue())
                .average()
                .orElse(0);
        VendorProfile vendor = vendorRepository.findById(vendorId);
        if (vendor != null) {
            vendor.avgRating = BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
            vendor.reviewCount = reviews.size();
        }
    }
}
