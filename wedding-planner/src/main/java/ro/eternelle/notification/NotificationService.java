package ro.eternelle.notification;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import static jakarta.transaction.Transactional.TxType.NOT_SUPPORTED;
import static jakarta.transaction.Transactional.TxType.REQUIRES_NEW;
import ro.eternelle.couple.CoupleProfile;
import ro.eternelle.user.User;
import ro.eternelle.user.UserRepository;
import ro.eternelle.vendor.VendorProfile;
import ro.eternelle.websocket.WebSocketService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class NotificationService {

    @Inject NotificationRepository notificationRepository;
    @Inject UserRepository userRepository;
    @Inject WebSocketService webSocketService;

    @Transactional(NOT_SUPPORTED)
    public List<NotificationDTO> getForUser(UUID userId) {
        return notificationRepository.findByUser(userId)
                .stream().map(NotificationDTO::from).collect(Collectors.toList());
    }

    @Transactional(NOT_SUPPORTED)
    public long countUnread(UUID userId) {
        return notificationRepository.countUnread(userId);
    }

    @Transactional(REQUIRES_NEW)
    public Notification create(UUID userId, String type, String title, String body, UUID entityId) {
        User user = userRepository.findById(userId);
        if (user == null) return null;
        Notification n = new Notification();
        n.user = user;
        n.type = type;
        n.title = title;
        n.body = body;
        n.entityId = entityId != null ? entityId.toString() : null;
        notificationRepository.persist(n);
        // Push real-time delivery to the recipient's browser
        webSocketService.broadcast("user:" + userId.toString(), "NOTIFICATION", n.id.toString());
        return n;
    }

    @Transactional
    public void markRead(UUID notificationId, UUID userId) {
        Notification n = notificationRepository.findById(notificationId);
        if (n != null && n.user.id.equals(userId)) {
            n.read = true;
        }
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.findUnreadByUser(userId)
                .forEach(n -> n.read = true);
    }

    //  Lead notifications 

    public void notifyNewLead(UUID vendorUserId, CoupleProfile couple, UUID leadId) {
        String coupleName = buildCoupleName(couple);
        create(vendorUserId, "NEW_LEAD", "New Enquiry",
                coupleName + " sent you a new enquiry", leadId);
    }

    public void notifyLeadAccepted(UUID coupleUserId, VendorProfile vendor, UUID leadId) {
        create(coupleUserId, "LEAD_ACCEPTED", "Enquiry Accepted",
                vendor.businessName + " accepted your enquiry and is ready to chat!", leadId);
    }

    public void notifyLeadDeclined(UUID coupleUserId, VendorProfile vendor, UUID leadId) {
        create(coupleUserId, "LEAD_DECLINED", "Enquiry Declined",
                vendor.businessName + " is unavailable for your enquiry.", leadId);
    }

    public void notifyNewOffer(UUID coupleUserId, VendorProfile vendor, UUID offerId) {
        create(coupleUserId, "NEW_OFFER", "New Offer",
                vendor.businessName + " sent you an offer!", offerId);
    }

    public void notifyOfferAccepted(UUID vendorUserId, CoupleProfile couple, UUID offerId) {
        String coupleName = buildCoupleName(couple);
        create(vendorUserId, "OFFER_ACCEPTED", "Offer Accepted",
                coupleName + " accepted your offer!", offerId);
    }

    public void notifyRevisionRequested(UUID vendorUserId, CoupleProfile couple, UUID leadId) {
        String coupleName = buildCoupleName(couple);
        create(vendorUserId, "OFFER_REVISION_REQUESTED", "Revision Requested",
                coupleName + " has requested a revised offer.", leadId);
    }

    public void notifyNewMessage(UUID recipientUserId, String senderName, UUID conversationId) {
        create(recipientUserId, "NEW_MESSAGE", "New Message",
                senderName + " sent you a message", conversationId);
    }

    // Booking management notifications

    public void notifyBookingCancelled(UUID coupleUserId, VendorProfile vendor, UUID bookingId) {
        create(coupleUserId, "BOOKING_CANCELLED",
                "Booking Cancelled",
                vendor.businessName + " has cancelled your booking.",
                bookingId);
    }

    public void notifyRescheduleProposed(UUID coupleUserId, VendorProfile vendor,
                                          java.time.LocalDate proposedDate, UUID bookingId) {
        create(coupleUserId, "BOOKING_RESCHEDULE_PROPOSED",
                "Reschedule Proposed",
                vendor.businessName + " proposed a new wedding date: " + proposedDate,
                bookingId);
    }

    public void notifyRescheduleAccepted(UUID vendorUserId, CoupleProfile couple, UUID bookingId) {
        create(vendorUserId, "BOOKING_RESCHEDULE_ACCEPTED",
                "Reschedule Accepted",
                buildCoupleName(couple) + " accepted the new date.",
                bookingId);
    }

    public void notifyRescheduleDeclined(UUID vendorUserId, CoupleProfile couple, UUID bookingId) {
        create(vendorUserId, "BOOKING_RESCHEDULE_DECLINED",
                "Reschedule Declined",
                buildCoupleName(couple) + " declined the new date. Original date kept.",
                bookingId);
    }

    //  helpers 

    private String buildCoupleName(CoupleProfile couple) {
        if (couple == null) return "A couple";
        String p1 = couple.partner1Name != null ? couple.partner1Name : "";
        String p2 = couple.partner2Name != null ? couple.partner2Name : "";
        return p2.isEmpty() ? p1 : p1 + " & " + p2;
    }
}
