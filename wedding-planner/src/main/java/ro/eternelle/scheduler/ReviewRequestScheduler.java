package ro.eternelle.scheduler;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import ro.eternelle.booking.Booking;
import ro.eternelle.booking.BookingRepository;
import ro.eternelle.notification.NotificationService;
import ro.eternelle.review.ReviewRepository;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class ReviewRequestScheduler {

    @Inject BookingRepository bookingRepository;
    @Inject ReviewRepository reviewRepository;
    @Inject NotificationService notificationService;

    /**
     * Runs daily — ask couples to review vendors 3 days after their wedding.
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void requestReviews() {
        LocalDate threshold = LocalDate.now().minusDays(3);
        List<Booking> eligible = bookingRepository.findAll().stream()
                .filter(b -> b.weddingDate != null && b.weddingDate.equals(threshold))
                .filter(b -> !reviewRepository.existsByBooking(b.id))
                .toList();

        for (Booking booking : eligible) {
            notificationService.create(
                    booking.lead.couple.user.id,
                    "review_request",
                    "How was your experience?",
                    "Please leave a review for " + booking.lead.vendor.businessName,
                    booking.id
            );
        }
    }
}
