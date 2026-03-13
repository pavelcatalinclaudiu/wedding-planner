package ro.eternelle.booking;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ro.eternelle.budget.BudgetRepository;
import ro.eternelle.exception.BusinessException;
import ro.eternelle.lead.LeadStatus;
import ro.eternelle.notification.NotificationService;
import ro.eternelle.review.ReviewRepository;
import ro.eternelle.vendor.AvailabilityRepository;
import ro.eternelle.vendor.VendorAnalyticsService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class BookingService {

    @Inject BookingRepository      bookingRepository;
    @Inject ReviewRepository        reviewRepository;
    @Inject NotificationService     notificationService;
    @Inject BudgetRepository        budgetRepository;
    @Inject AvailabilityRepository  availabilityRepository;
    @Inject VendorAnalyticsService  vendorAnalyticsService;

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

    @Transactional
    public BookingDTO cancelBooking(UUID bookingId, UUID actorVendorId) {
        Booking b = findAndVerifyVendor(bookingId, actorVendorId);
        if (b.status == BookingStatus.CANCELLED)
            throw new BusinessException("Booking is already cancelled");
        b.status = BookingStatus.CANCELLED;
        if (b.lead != null) {
            b.lead.status = LeadStatus.CANCELLED;
        }
        if (b.lead != null && b.lead.couple != null && b.lead.couple.user != null
                && b.lead.vendor != null) {
            notificationService.notifyBookingCancelled(
                    b.lead.couple.user.id, b.lead.vendor, b.id);
        }
        // Remove the auto-created budget item for this booking
        try {
            if (b.lead != null) budgetRepository.deleteByLeadId(b.lead.id);
        } catch (Exception ignored) {}
        // Remove the manual availability block that was added when the offer was accepted
        try {
            if (b.weddingDate != null && b.lead != null && b.lead.vendor != null) {
                availabilityRepository
                        .findByVendorAndDate(b.lead.vendor.id, b.weddingDate)
                        .ifPresent(a -> availabilityRepository.delete(a));
            }
        } catch (Exception ignored) {}
        // Invalidate analytics cache so the conversion rate reflects the cancellation immediately
        try {
            if (b.lead != null && b.lead.vendor != null) {
                vendorAnalyticsService.invalidateOverview(b.lead.vendor.id);
            }
        } catch (Exception ignored) {}
        return toDTO(b);
    }

    @Transactional
    public BookingDTO proposeReschedule(UUID bookingId, UUID actorVendorId,
                                        LocalDate proposedDate, String note) {
        Booking b = findAndVerifyVendor(bookingId, actorVendorId);
        if (b.status == BookingStatus.CANCELLED)
            throw new BusinessException("Cannot reschedule a cancelled booking");
        b.status       = BookingStatus.RESCHEDULE_PENDING;
        b.proposedDate = proposedDate;
        b.proposedNote = note;
        if (b.lead != null && b.lead.couple != null && b.lead.couple.user != null
                && b.lead.vendor != null) {
            notificationService.notifyRescheduleProposed(
                    b.lead.couple.user.id, b.lead.vendor, proposedDate, b.id);
        }
        return toDTO(b);
    }

    @Transactional
    public BookingDTO respondReschedule(UUID bookingId, UUID actorCoupleId, boolean accept) {
        Booking b = bookingRepository.findByIdOptional(bookingId)
                .orElseThrow(() -> new BusinessException("Booking not found"));
        if (b.lead == null || b.lead.couple == null
                || !b.lead.couple.id.equals(actorCoupleId))
            throw new BusinessException("Not authorized");
        if (b.status != BookingStatus.RESCHEDULE_PENDING)
            throw new BusinessException("No reschedule proposal pending");
        if (accept) {
            b.weddingDate = b.proposedDate;
            b.status      = BookingStatus.CONFIRMED;
            if (b.lead.vendor != null && b.lead.vendor.user != null) {
                notificationService.notifyRescheduleAccepted(
                        b.lead.vendor.user.id, b.lead.couple, b.id);
            }
        } else {
            b.status = BookingStatus.CONFIRMED;
            if (b.lead.vendor != null && b.lead.vendor.user != null) {
                notificationService.notifyRescheduleDeclined(
                        b.lead.vendor.user.id, b.lead.couple, b.id);
            }
        }
        b.proposedDate = null;
        b.proposedNote = null;
        return toDTO(b);
    }

    private Booking findAndVerifyVendor(UUID bookingId, UUID vendorId) {
        Booking b = bookingRepository.findByIdOptional(bookingId)
                .orElseThrow(() -> new BusinessException("Booking not found"));
        if (b.lead == null || b.lead.vendor == null
                || !b.lead.vendor.id.equals(vendorId))
            throw new BusinessException("Not authorized");
        return b;
    }
}
