package ro.eternelle.booking;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ro.eternelle.exception.BusinessException;
import ro.eternelle.review.ReviewRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class BookingService {

    @Inject BookingRepository   bookingRepository;
    @Inject ReviewRepository    reviewRepository;

    private BookingDTO toDTO(Booking b) {
        return BookingDTO.from(b, reviewRepository.existsByBooking(b.id));
    }

    public BookingDTO getByLead(UUID leadId) {
        Booking b = bookingRepository.findByLead(leadId)
                .orElseThrow(() -> new BusinessException("Booking not found"));
        return toDTO(b);
    }

    public List<BookingDTO> getByCouple(UUID coupleId) {
        return bookingRepository.findByCouple(coupleId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<BookingDTO> getByVendor(UUID vendorId) {
        return bookingRepository.findByVendor(vendorId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public BookingDTO recordDeposit(UUID bookingId, java.math.BigDecimal amount) {
        Booking b = bookingRepository.findByIdOptional(bookingId)
                .orElseThrow(() -> new BusinessException("Booking not found"));
        b.depositPaid   = amount;
        b.depositPaidAt = Instant.now();
        return toDTO(b);
    }
}
