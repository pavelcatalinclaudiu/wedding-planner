package ro.eternelle.messaging;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ro.eternelle.couple.CoupleProfile;
import ro.eternelle.couple.CoupleRepository;
import ro.eternelle.exception.BusinessException;
import ro.eternelle.lead.Lead;
import ro.eternelle.lead.LeadRepository;
import ro.eternelle.user.User;
import ro.eternelle.user.UserRepository;
import ro.eternelle.vendor.VendorAnalyticsService;
import ro.eternelle.vendor.VendorRepository;
import ro.eternelle.websocket.WebSocketService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class MessageService {

    @Inject ThreadRepository threadRepository;
    @Inject MessageRepository messageRepository;
    @Inject LeadRepository leadRepository;
    @Inject UserRepository userRepository;
    @Inject ThreadParticipantRepository participantRepository;
    @Inject CoupleRepository coupleRepository;
    @Inject VendorRepository vendorRepository;
    @Inject WebSocketService webSocketService;
    @Inject VendorAnalyticsService vendorAnalyticsService;

    // ─── Direct thread (lead-based) ──────────────────────────────────────────

    public ThreadDTO getOrCreateThread(UUID leadId, UUID viewerUserId) {
        Thread t = threadRepository.findByLead(leadId).orElseGet(() -> createThreadForLead(leadId));
        List<Message> msgs = messageRepository.findByThread(t.id);
        Message last = msgs.isEmpty() ? null : msgs.get(msgs.size() - 1);
        long unread = messageRepository.countUnread(t.id, viewerUserId);
        return ThreadDTO.from(t, viewerUserId, unread, last);
    }

    @Transactional
    Thread createThreadForLead(UUID leadId) {
        Lead lead = leadRepository.findById(leadId);
        if (lead == null) throw new BusinessException("Lead not found");

        Thread thread = new Thread();
        thread.lead = lead;
        thread.threadType = ThreadType.DIRECT;
        threadRepository.persist(thread);

        addParticipant(thread, lead.couple.user);
        addParticipant(thread, lead.vendor.user);

        return thread;
    }

    // ─── Group thread (couple-based) ─────────────────────────────────────────

    /** Get or create the single GROUP thread for this couple user. */
    @Transactional
    public ThreadDTO getGroupThreadForCouple(UUID coupleUserId) {
        CoupleProfile couple = coupleRepository.findByUserId(coupleUserId)
                .orElseThrow(() -> new BusinessException("Couple profile not found"));

        Thread t = threadRepository.findGroupByCouple(couple.id)
                .orElseGet(() -> createGroupThread(couple));

        List<ThreadParticipant> participants = participantRepository.findByThread(t.id);
        List<ThreadParticipantDTO> participantDTOs = buildParticipantDTOs(participants);

        List<Message> msgs = messageRepository.findByThread(t.id);
        Message last = msgs.isEmpty() ? null : msgs.get(msgs.size() - 1);
        long unread = messageRepository.countUnread(t.id, coupleUserId);

        return ThreadDTO.from(t, coupleUserId, unread, last, participantDTOs);
    }

    /** List all GROUP threads the vendor participates in. */
    public List<ThreadDTO> getGroupThreadsForVendor(UUID vendorUserId) {
        return threadRepository.findGroupThreadsByUser(vendorUserId).stream().map(t -> {
            List<ThreadParticipant> participants = participantRepository.findByThread(t.id);
            List<ThreadParticipantDTO> participantDTOs = buildParticipantDTOs(participants);
            List<Message> msgs = messageRepository.findByThread(t.id);
            Message last = msgs.isEmpty() ? null : msgs.get(msgs.size() - 1);
            long unread = messageRepository.countUnread(t.id, vendorUserId);
            return ThreadDTO.from(t, vendorUserId, unread, last, participantDTOs);
        }).toList();
    }

    /** Create a new GROUP thread for the couple (couple user is added as first participant). */
    @Transactional
    Thread createGroupThread(CoupleProfile couple) {
        Thread t = new Thread();
        t.couple = couple;
        t.threadType = ThreadType.GROUP;
        threadRepository.persist(t);
        addParticipant(t, couple.user);
        return t;
    }

    /**
     * Idempotent: add the vendor's user to the couple's group thread.
     * Called from OfferService when a booking is confirmed.
     */
    @Transactional
    public void ensureVendorInGroupThread(CoupleProfile couple, User vendorUser) {
        Thread t = threadRepository.findGroupByCouple(couple.id)
                .orElseGet(() -> createGroupThread(couple));

        boolean alreadyPresent = participantRepository
                .findByThreadAndUser(t.id, vendorUser.id).isPresent();

        if (!alreadyPresent) {
            addParticipant(t, vendorUser);

            // Post a SYSTEM message so all participants see who joined
            String vendorName = vendorRepository.findByUserId(vendorUser.id)
                    .map(v -> v.businessName)
                    .orElse(vendorUser.email);

            Message sys = new Message();
            sys.thread = t;
            sys.sender = vendorUser;
            sys.content = vendorName + " joined the group chat";
            sys.type = MessageType.SYSTEM;
            messageRepository.persist(sys);
            t.lastMessageAt = Instant.now();

            webSocketService.broadcast("thread:" + t.id, "PARTICIPANT_JOINED", vendorUser.id.toString());
        }
    }

    /** Return participants for any thread (used by the detail endpoint). */
    public List<ThreadParticipantDTO> getParticipants(UUID threadId) {
        return buildParticipantDTOs(participantRepository.findByThread(threadId));
    }

    /** Couple removes a vendor participant from the group thread. */
    @Transactional
    public void removeParticipant(UUID threadId, UUID participantId, UUID requesterId) {
        Thread t = threadRepository.findById(threadId);
        if (t == null) throw new BusinessException("Thread not found");
        if (t.threadType != ThreadType.GROUP) throw new BusinessException("Not a group thread");

        // Only the couple owner may remove participants
        CoupleProfile couple = coupleRepository.findByUserId(requesterId)
                .orElseThrow(() -> new BusinessException("Only the couple can remove participants"));
        if (!couple.id.equals(t.couple.id)) throw new BusinessException("Not your group thread");

        // participantId here is the ThreadParticipant row id
        ThreadParticipant tp = participantRepository.findById(participantId);
        if (tp == null || !tp.thread.id.equals(threadId))
            throw new BusinessException("Participant not found in this thread");

        participantRepository.delete(tp);
        webSocketService.broadcast("thread:" + threadId, "PARTICIPANT_REMOVED", participantId.toString());
    }

    /** Vendor leaves a group thread (removes their own ThreadParticipant row). */
    @Transactional
    public void leaveThread(UUID threadId, UUID userId) {
        Optional<ThreadParticipant> tpOpt = participantRepository.findByThreadAndUser(threadId, userId);
        tpOpt.ifPresent(tp -> {
            participantRepository.delete(tp);
            webSocketService.broadcast("thread:" + threadId, "PARTICIPANT_LEFT", userId.toString());
        });
    }

    // ─── Shared methods ───────────────────────────────────────────────────────

    private void addParticipant(Thread thread, User user) {
        ThreadParticipant p = new ThreadParticipant();
        p.thread = thread;
        p.user = user;
        p.persist();
    }

    private List<ThreadParticipantDTO> buildParticipantDTOs(List<ThreadParticipant> participants) {
        return participants.stream().map(tp -> {
            String name;
            String role;
            String avatar = null;

            var vendor = vendorRepository.findByUserId(tp.user.id).orElse(null);
            if (vendor != null) {
                name = vendor.businessName;
                role = "VENDOR";
                avatar = vendor.profilePicture;
            } else {
                var couple = coupleRepository.findByUserId(tp.user.id).orElse(null);
                if (couple != null) {
                    name = couple.partner1Name + " & " + couple.partner2Name;
                    role = "COUPLE";
                } else {
                    name = tp.user.email;
                    role = "UNKNOWN";
                }
            }
            return ThreadParticipantDTO.from(tp, name, role, avatar);
        }).toList();
    }

    public List<MessageDTO> getMessages(UUID threadId, int page, int size) {
        return messageRepository.findByThreadPaged(threadId, page, size)
                .stream().map(MessageDTO::from).toList();
    }

    public List<ThreadDTO> getThreadsForUser(UUID userId) {
        return threadRepository.findByUser(userId).stream().map(t -> {
            List<Message> msgs = messageRepository.findByThread(t.id);
            Message last = msgs.isEmpty() ? null : msgs.get(msgs.size() - 1);
            long unread = messageRepository.countUnread(t.id, userId);
            return ThreadDTO.from(t, userId, unread, last);
        }).toList();
    }

    @Transactional
    public MessageDTO sendMessage(UUID threadId, UUID senderId, String content, MessageType type) {
        Thread thread = threadRepository.findById(threadId);
        if (thread == null) throw new BusinessException("Thread not found");

        User sender = userRepository.findById(senderId);
        if (sender == null) throw new BusinessException("User not found");

        Message message = new Message();
        message.thread = thread;
        message.sender = sender;
        message.content = content;
        message.type = type != null ? type : MessageType.TEXT;
        messageRepository.persist(message);

        thread.lastMessageAt = Instant.now();

        if (thread.lead != null) {
            webSocketService.broadcast("deal:" + thread.lead.id, "NEW_MESSAGE", message.id.toString());
        } else {
            webSocketService.broadcast("thread:" + threadId, "NEW_MESSAGE", message.id.toString());
        }

        if (thread.lead != null && thread.lead.vendor != null) {
            vendorAnalyticsService.invalidateOverview(thread.lead.vendor.id);
        }

        return MessageDTO.from(message);
    }

    @Transactional
    public void markRead(UUID threadId, UUID userId) {
        messageRepository.findByThread(threadId).stream()
                .filter(m -> !m.readBy.contains(userId))
                .forEach(m -> m.readBy.add(userId));
    }
}
