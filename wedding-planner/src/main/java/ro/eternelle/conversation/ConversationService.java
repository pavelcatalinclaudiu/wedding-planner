package ro.eternelle.conversation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ro.eternelle.couple.CoupleProfile;
import ro.eternelle.couple.CoupleRepository;
import ro.eternelle.exception.BusinessException;
import ro.eternelle.lead.Lead;
import ro.eternelle.lead.LeadRepository;
import ro.eternelle.notification.NotificationService;
import ro.eternelle.user.User;
import ro.eternelle.user.UserRepository;
import ro.eternelle.vendor.VendorProfile;
import ro.eternelle.vendor.VendorRepository;
import ro.eternelle.websocket.WebSocketService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class ConversationService {

    @Inject ConversationRepository       conversationRepository;
    @Inject ConversationMessageRepository messageRepository;
    @Inject LeadRepository               leadRepository;
    @Inject UserRepository               userRepository;
    @Inject CoupleRepository             coupleRepository;
    @Inject VendorRepository             vendorRepository;
    @Inject NotificationService          notificationService;
    @Inject WebSocketService             webSocketService;

    // ── Get or fetch conversation for a lead ──────────────────────────────────

    public ConversationDTO getByLead(UUID leadId, UUID actorUserId) {
        Conversation conv = conversationRepository.findByLead(leadId)
                .orElseThrow(() -> new BusinessException("Conversation not found for this lead"));
        assertParticipant(conv, actorUserId);
        return ConversationDTO.from(conv);
    }

    // ── List all conversations for the logged-in user ─────────────────────────

    public List<ConversationDTO> getMyConversations(UUID userId, String role) {
        List<Conversation> convs;
        if ("COUPLE".equals(role)) {
            CoupleProfile couple = coupleRepository.findByUserId(userId)
                    .orElseThrow(() -> new BusinessException("Couple profile not found"));
            convs = conversationRepository.findByCouple(couple.id);
        } else {
            VendorProfile vendor = vendorRepository.findByUserId(userId)
                    .orElseThrow(() -> new BusinessException("Vendor profile not found"));
            convs = conversationRepository.findByVendor(vendor.id);
        }
        return convs.stream().map(ConversationDTO::from).collect(Collectors.toList());
    }

    // ── Get messages ──────────────────────────────────────────────────────────

    public List<ConversationMessageDTO> getMessages(UUID conversationId, UUID actorUserId) {
        Conversation conv = findById(conversationId);
        assertParticipant(conv, actorUserId);
        return messageRepository.findByConversation(conversationId)
                .stream()
                .map(ConversationMessageDTO::from)
                .collect(Collectors.toList());
    }

    // ── Send message ──────────────────────────────────────────────────────────

    @Transactional
    public ConversationMessageDTO sendMessage(UUID conversationId, UUID actorUserId, String role, String content) {
        Conversation conv = findById(conversationId);
        assertParticipant(conv, actorUserId);

        User sender = userRepository.findById(actorUserId);
        if (sender == null) throw new BusinessException("User not found");

        ConversationMessage msg = new ConversationMessage();
        msg.conversation = conv;
        msg.sender       = sender;
        msg.senderRole   = ConversationMessage.SenderRole.valueOf(role);
        msg.content      = content;
        msg.sentAt       = Instant.now();
        messageRepository.persist(msg);

        conv.lastMessageAt = msg.sentAt;

        // Push real-time update to both participants via WebSocket
        if (conv.lead != null) {
            webSocketService.broadcast("deal:" + conv.lead.id.toString(), "NEW_MESSAGE", ConversationMessageDTO.from(msg));
        }

        // Notify the other party
        try {
            UUID recipientUserId;
            String senderName;
            if ("COUPLE".equals(role)) {
                recipientUserId = conv.vendor.user.id;
                senderName      = buildCoupleName(conv.couple);
            } else {
                recipientUserId = conv.couple.user.id;
                senderName      = conv.vendor.businessName;
            }
            notificationService.notifyNewMessage(recipientUserId, senderName, conversationId);
        } catch (Exception ignored) {}

        return ConversationMessageDTO.from(msg);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Conversation findById(UUID id) {
        return conversationRepository.findByIdOptional(id)
                .orElseThrow(() -> new BusinessException("Conversation not found"));
    }

    private void assertParticipant(Conversation conv, UUID userId) {
        boolean isCouple = conv.couple != null && userId.equals(conv.couple.user.id);
        boolean isVendor = conv.vendor != null && userId.equals(conv.vendor.user.id);
        if (!isCouple && !isVendor) {
            throw new BusinessException("Access denied to this conversation");
        }
    }

    private String buildCoupleName(CoupleProfile couple) {
        if (couple == null) return "A couple";
        String p1 = couple.partner1Name != null ? couple.partner1Name : "";
        String p2 = couple.partner2Name != null ? couple.partner2Name : "";
        return p2.isEmpty() ? p1 : p1 + " & " + p2;
    }

    // ── System messages (posted programmatically, not by a user typing) ────────

    @Transactional(jakarta.transaction.Transactional.TxType.REQUIRES_NEW)
    public void postSystemMessage(UUID leadId, ConversationMessageType type,
                                  String content, UUID senderUserId, String senderRole) {
        Conversation conv = conversationRepository.findByLead(leadId).orElse(null);
        if (conv == null) return; // conversation not yet open — skip silently

        User sender = userRepository.findById(senderUserId);
        if (sender == null) return;

        ConversationMessage msg = new ConversationMessage();
        msg.conversation = conv;
        msg.sender       = sender;
        msg.senderRole   = ConversationMessage.SenderRole.valueOf(senderRole);
        msg.content      = content;
        msg.type         = type;
        msg.sentAt       = Instant.now();
        messageRepository.persist(msg);

        conv.lastMessageAt = msg.sentAt;

        if (conv.lead != null) {
            webSocketService.broadcast("deal:" + conv.lead.id.toString(), "NEW_MESSAGE", ConversationMessageDTO.from(msg));
        }
    }
}
