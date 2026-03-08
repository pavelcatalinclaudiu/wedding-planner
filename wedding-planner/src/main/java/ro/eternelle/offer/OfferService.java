package ro.eternelle.offer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import ro.eternelle.booking.Booking;
import ro.eternelle.booking.BookingRepository;
import ro.eternelle.budget.BudgetItem;
import ro.eternelle.budget.BudgetRepository;
import ro.eternelle.couple.CoupleProfile;
import ro.eternelle.conversation.ConversationMessageType;
import ro.eternelle.conversation.ConversationService;
import ro.eternelle.couple.CoupleRepository;
import ro.eternelle.exception.BusinessException;
import ro.eternelle.lead.Lead;
import ro.eternelle.lead.LeadRepository;
import ro.eternelle.lead.LeadStatus;
import ro.eternelle.notification.NotificationService;
import ro.eternelle.vendor.AvailabilityRepository;
import ro.eternelle.vendor.VendorAnalyticsService;
import ro.eternelle.vendor.VendorAvailability;
import ro.eternelle.messaging.MessageService;
import ro.eternelle.vendor.VendorRepository;
import ro.eternelle.websocket.WebSocketService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class OfferService {

    @Inject OfferRepository        offerRepository;
    @Inject LeadRepository         leadRepository;
    @Inject BookingRepository      bookingRepository;
    @Inject BudgetRepository       budgetRepository;
    @Inject CoupleRepository       coupleRepository;
    @Inject VendorRepository       vendorRepository;
    @Inject NotificationService    notificationService;
    @Inject VendorAnalyticsService vendorAnalyticsService;
    @Inject AvailabilityRepository availabilityRepository;
    @Inject ConversationService    conversationService;
    @Inject WebSocketService       webSocketService;
    @Inject MessageService         messageService;
    @Inject EntityManager          em;

    // ── Create offer (vendor action) ──────────────────────────────────────────

    @Transactional
    public OfferDTO createOffer(UUID leadId, UUID actorUserId, CreateOfferRequest req) {
        Lead lead = getLeadOrThrow(leadId);

        // Only the vendor of the lead may send an offer
        if (!lead.vendor.user.id.equals(actorUserId)) {
            throw new BusinessException("Only the assigned vendor may create an offer");
        }
        if (lead.status == LeadStatus.DECLINED || lead.status == LeadStatus.BOOKED) {
            throw new BusinessException("Cannot create offer for a lead in status " + lead.status);
        }

        Offer offer = new Offer();
        offer.lead           = lead;
        offer.conversation   = req.conversationId != null
                ? em.getReference(ro.eternelle.conversation.Conversation.class, req.conversationId)
                : null;
        offer.vendor         = lead.vendor;
        offer.packageDetails = req.packageDetails;
        offer.price          = req.price;
        offer.expiresAt      = req.expiresAt;
        offerRepository.persist(offer);

        // Advance lead to QUOTED
        lead.status = LeadStatus.QUOTED;

        // Notify couple
        try {
            notificationService.notifyNewOffer(lead.couple.user.id, lead.vendor, offer.id);
        } catch (Exception ignored) {}

        OfferDTO dto = OfferDTO.from(offer);
        broadcastOfferUpdated(lead.id, dto);
        return dto;
    }

    // ── Accept offer (couple action) ──────────────────────────────────────────

    @Transactional
    public OfferDTO acceptOffer(UUID offerId, UUID actorUserId) {
        Offer offer = getOfferOrThrow(offerId);

        // Only the couple of the lead may accept
        if (!offer.lead.couple.user.id.equals(actorUserId)) {
            throw new BusinessException("Only the couple may accept an offer");
        }
        if (offer.status != OfferStatus.PENDING) {
            throw new BusinessException("Offer is not pending");
        }

        offer.status = OfferStatus.ACCEPTED;

        // Mark lead as BOOKED
        Lead lead = offer.lead;
        lead.status = LeadStatus.BOOKED;

        // Create booking
        Booking booking = new Booking();
        booking.lead       = lead;
        booking.offer      = offer;
        booking.weddingDate = lead.eventDate;
        booking.totalPrice  = offer.price;
        bookingRepository.persist(booking);

        // Copy event date + vendor city to couple profile if not yet set
        CoupleProfile coupleProfile = lead.couple;
        if (coupleProfile.weddingDate == null && lead.eventDate != null) {
            coupleProfile.weddingDate = lead.eventDate;
        }
        if ((coupleProfile.weddingLocation == null || coupleProfile.weddingLocation.isBlank())
                && lead.vendor.city != null) {
            coupleProfile.weddingLocation = lead.vendor.city;
        }

        // Block vendor availability for the wedding date
        if (lead.eventDate != null) {
            boolean alreadyBlocked = availabilityRepository
                    .findByVendorAndDate(lead.vendor.id, lead.eventDate)
                    .isPresent();
            if (!alreadyBlocked) {
                VendorAvailability slot = new VendorAvailability();
                slot.vendor = lead.vendor;
                slot.date   = lead.eventDate;
                slot.reason = "Booked";
                availabilityRepository.persist(slot);
            }
        }

        // Invalidate analytics cache
        try {
            vendorAnalyticsService.invalidateOverview(lead.vendor.id);
        } catch (Exception ignored) {}

        // Notify vendor
        try {
            notificationService.notifyOfferAccepted(lead.vendor.user.id, lead.couple, offer.id);
        } catch (Exception ignored) {}

        // Add vendor to the couple's group planning chat
        try {
            messageService.ensureVendorInGroupThread(lead.couple, lead.vendor.user);
        } catch (Exception ignored) {}

        // Auto-add to couple's budget tracker
        try {
            String categoryLabel = lead.vendor.category != null
                    ? lead.vendor.category.displayName
                    : "Other";
            BudgetItem budgetItem = new BudgetItem();
            budgetItem.couple        = lead.couple;
            budgetItem.category      = categoryLabel;
            budgetItem.name          = categoryLabel;
            budgetItem.vendorName    = lead.vendor.businessName;
            budgetItem.estimatedCost = offer.price;
            budgetItem.actualCost    = offer.price;
            budgetItem.isPaid        = false;
            budgetItem.leadId        = lead.id;
            budgetRepository.persist(budgetItem);
        } catch (Exception ignored) {}

        OfferDTO dto = OfferDTO.from(offer);
        broadcastOfferUpdated(lead.id, dto);
        return dto;
    }

    // ── Decline offer (couple action) ───────────────────────────────────────────────────────────

    @Transactional
    public OfferDTO declineOffer(UUID offerId, UUID actorUserId) {
        Offer offer = getOfferOrThrow(offerId);

        if (!offer.lead.couple.user.id.equals(actorUserId)) {
            throw new BusinessException("Only the couple may decline an offer");
        }
        if (offer.status != OfferStatus.PENDING) {
            throw new BusinessException("Offer is not pending");
        }

        offer.status = OfferStatus.DECLINED;
        // Lead goes back to IN_DISCUSSION so vendor can revise or re-offer
        offer.lead.status = LeadStatus.IN_DISCUSSION;

        OfferDTO dto = OfferDTO.from(offer);
        broadcastOfferUpdated(offer.lead.id, dto);
        return dto;
    }

    // ── Mark viewed (couple action) ──────────────────────────────────────────────────────────────

    @Transactional
    public OfferDTO markViewed(UUID offerId, UUID actorUserId) {
        Offer offer = getOfferOrThrow(offerId);
        if (!offer.lead.couple.user.id.equals(actorUserId)) {
            throw new BusinessException("Only the couple may mark an offer as viewed");
        }
        // Only set once — don't overwrite original view timestamp
        if (offer.viewedAt == null) {
            offer.viewedAt = Instant.now();
        }
        return OfferDTO.from(offer);
    }

    // ── Request revision (couple action) ─────────────────────────────────────────────────────────

    @Transactional
    public OfferDTO requestRevision(UUID offerId, UUID actorUserId) {
        Offer offer = getOfferOrThrow(offerId);

        if (!offer.lead.couple.user.id.equals(actorUserId)) {
            throw new BusinessException("Only the couple may request a revision");
        }
        if (offer.status != OfferStatus.PENDING) {
            throw new BusinessException("Offer is not pending");
        }

        offer.status = OfferStatus.REVISED;
        // Lead goes back to IN_DISCUSSION so vendor can send a new offer
        offer.lead.status = LeadStatus.IN_DISCUSSION;

        // Post a system message in the conversation so it appears as a structured event in chat
        try {
            String payload = String.format("{\"offerId\":\"%s\"}", offerId);
            conversationService.postSystemMessage(
                    offer.lead.id,
                    ConversationMessageType.OFFER_REVISION_REQUEST,
                    payload,
                    actorUserId,
                    "COUPLE");
        } catch (Exception ignored) {}

        // Notify vendor
        try {
            notificationService.notifyRevisionRequested(
                    offer.lead.vendor.user.id, offer.lead.couple, offer.lead.id);
        } catch (Exception ignored) {}

        OfferDTO dto = OfferDTO.from(offer);
        broadcastOfferUpdated(offer.lead.id, dto);
        return dto;
    }

    // ── Read offers ───────────────────────────────────────────────────────────

    public List<OfferDTO> getByLead(UUID leadId) {
        return offerRepository.findByLead(leadId)
                .stream().map(OfferDTO::from).collect(Collectors.toList());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Offer getOfferOrThrow(UUID id) {
        return offerRepository.findByIdOptional(id)
                .orElseThrow(() -> new BusinessException("Offer not found"));
    }

    private Lead getLeadOrThrow(UUID id) {
        Lead l = leadRepository.findById(id);
        if (l == null) throw new BusinessException("Lead not found");
        return l;
    }

    private void broadcastOfferUpdated(UUID leadId, OfferDTO dto) {
        try {
            webSocketService.broadcast("deal:" + leadId, "OFFER_UPDATED", dto);
        } catch (Exception ignored) {}
    }
}
