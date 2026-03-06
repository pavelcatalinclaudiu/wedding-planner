package ro.eternelle.lead;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ro.eternelle.conversation.Conversation;
import ro.eternelle.conversation.ConversationMessage;
import ro.eternelle.conversation.ConversationMessageRepository;
import ro.eternelle.conversation.ConversationMessageType;
import ro.eternelle.conversation.ConversationRepository;
import ro.eternelle.couple.CoupleProfile;
import ro.eternelle.couple.CoupleRepository;
import ro.eternelle.exception.BusinessException;
import ro.eternelle.exception.UnauthorizedException;
import ro.eternelle.notification.NotificationService;
import ro.eternelle.vendor.VendorProfile;
import ro.eternelle.vendor.VendorRepository;
import ro.eternelle.websocket.WebSocketService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class LeadService {

    @Inject LeadRepository leadRepository;
    @Inject CoupleRepository coupleRepository;
    @Inject VendorRepository vendorRepository;
    @Inject ConversationRepository conversationRepository;
    @Inject ConversationMessageRepository conversationMessageRepository;
    @Inject NotificationService notificationService;
    @Inject ro.eternelle.vendor.VendorAnalyticsService vendorAnalyticsService;
    @Inject WebSocketService webSocketService;
    @Inject ObjectMapper objectMapper;

    @Transactional
    public LeadDTO createLead(UUID coupleUserId, UUID vendorId,
                              LocalDate eventDate, BigDecimal budget, String message) {
        CoupleProfile couple = coupleRepository.findByUserId(coupleUserId)
                .orElseThrow(() -> new BusinessException("Couple profile not found"));
        VendorProfile vendor = vendorRepository.findById(vendorId);
        if (vendor == null) throw new BusinessException("Vendor not found");

        Lead lead = new Lead();
        lead.couple = couple;
        lead.vendor = vendor;
        lead.eventDate = eventDate;
        lead.budget = budget;
        lead.message = message;
        lead.status = LeadStatus.NEW;
        leadRepository.persist(lead);

        vendorAnalyticsService.invalidateOverview(vendor.id);

        // Notify vendor
        try {
            notificationService.notifyNewLead(vendor.user.id, couple, lead.id);
        } catch (Exception ignored) {}

        LeadDTO dto = LeadDTO.from(lead);
        broadcastLeadUpdate(lead.id, dto);
        return dto;
    }

    public List<LeadDTO> getLeadsForVendor(UUID vendorUserId, LeadStatus statusFilter) {
        VendorProfile vendor = vendorRepository.findByUserId(vendorUserId)
                .orElseThrow(() -> new BusinessException("Vendor not found"));
        List<Lead> leads = statusFilter != null
                ? leadRepository.findByVendorAndStatus(vendor.id, statusFilter)
                : leadRepository.findByVendor(vendor.id);
        return leads.stream().map(LeadDTO::from).collect(Collectors.toList());
    }

    public List<LeadDTO> getLeadsForCouple(UUID coupleUserId) {
        CoupleProfile couple = coupleRepository.findByUserId(coupleUserId)
                .orElseThrow(() -> new BusinessException("Couple profile not found"));
        return leadRepository.findByCouple(couple.id).stream()
                .map(LeadDTO::from).collect(Collectors.toList());
    }

    public LeadDTO getLead(UUID leadId, UUID actorUserId) {
        Lead lead = requireLead(leadId);
        requireParticipant(lead, actorUserId);
        return LeadDTO.from(lead);
    }

    @Transactional
    public LeadDTO markViewed(UUID leadId, UUID vendorUserId) {
        VendorProfile vendor = vendorRepository.findByUserId(vendorUserId)
                .orElseThrow(() -> new BusinessException("Vendor not found"));
        Lead lead = requireLead(leadId);
        if (!lead.vendor.id.equals(vendor.id)) throw new UnauthorizedException("Not authorized");
        if (lead.status == LeadStatus.NEW) {
            lead.status = LeadStatus.VIEWED;
            lead.updatedAt = Instant.now();
        }
        LeadDTO dto = LeadDTO.from(lead);
        broadcastLeadUpdate(lead.id, dto);
        return dto;
    }

    @Transactional
    public LeadDTO acceptLead(UUID leadId, UUID vendorUserId) {
        VendorProfile vendor = vendorRepository.findByUserId(vendorUserId)
                .orElseThrow(() -> new BusinessException("Vendor not found"));
        Lead lead = requireLead(leadId);
        if (!lead.vendor.id.equals(vendor.id)) throw new UnauthorizedException("Not authorized");
        if (lead.status == LeadStatus.DECLINED) throw new BusinessException("Lead already declined");

        lead.status = LeadStatus.IN_DISCUSSION;
        lead.updatedAt = Instant.now();

        // Create conversation if one does not already exist
        if (conversationRepository.findByLead(leadId).isEmpty()) {
            Conversation conversation = new Conversation();
            conversation.lead = lead;
            conversation.couple = lead.couple;
            conversation.vendor = vendor;
            conversationRepository.persist(conversation);

            // Seed the conversation with the couple's original enquiry message
            if (lead.message != null && !lead.message.isBlank()) {
                ConversationMessage seedMsg = new ConversationMessage();
                seedMsg.conversation = conversation;
                seedMsg.sender       = lead.couple.user;
                seedMsg.senderRole   = ConversationMessage.SenderRole.COUPLE;
                seedMsg.content      = lead.message;
                seedMsg.type         = ConversationMessageType.TEXT;
                seedMsg.sentAt       = lead.createdAt != null ? lead.createdAt : Instant.now();
                conversationMessageRepository.persist(seedMsg);
                conversation.lastMessageAt = seedMsg.sentAt;
            }
        }

        vendorAnalyticsService.invalidateOverview(vendor.id);

        // Notify couple
        try {
            notificationService.notifyLeadAccepted(lead.couple.user.id, vendor, leadId);
        } catch (Exception ignored) {}

        LeadDTO dto = LeadDTO.from(lead);
        broadcastLeadUpdate(leadId, dto);
        return dto;
    }

    @Transactional
    public LeadDTO declineLead(UUID leadId, UUID vendorUserId) {
        VendorProfile vendor = vendorRepository.findByUserId(vendorUserId)
                .orElseThrow(() -> new BusinessException("Vendor not found"));
        Lead lead = requireLead(leadId);
        if (!lead.vendor.id.equals(vendor.id)) throw new UnauthorizedException("Not authorized");

        lead.status = LeadStatus.DECLINED;
        lead.updatedAt = Instant.now();

        vendorAnalyticsService.invalidateOverview(vendor.id);

        // Notify couple
        try {
            notificationService.notifyLeadDeclined(lead.couple.user.id, vendor, leadId);
        } catch (Exception ignored) {}

        LeadDTO dto = LeadDTO.from(lead);
        broadcastLeadUpdate(leadId, dto);
        return dto;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Lead requireLead(UUID leadId) {
        Lead lead = leadRepository.findById(leadId);
        if (lead == null) throw new BusinessException("Lead not found");
        return lead;
    }

    private void requireParticipant(Lead lead, UUID actorUserId) {
        boolean isCoupleUser = lead.couple != null && lead.couple.user != null
                && lead.couple.user.id.equals(actorUserId);
        boolean isVendorUser = lead.vendor != null && lead.vendor.user != null
                && lead.vendor.user.id.equals(actorUserId);
        if (!isCoupleUser && !isVendorUser) throw new UnauthorizedException("Not a participant");
    }

    private void broadcastLeadUpdate(UUID leadId, LeadDTO dto) {
        try {
            String payload = objectMapper.writeValueAsString(dto);
            webSocketService.broadcast("lead:" + leadId, "LEAD_UPDATED", payload);
        } catch (Exception ignored) {}
    }
}
