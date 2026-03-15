package ro.eternelle.messaging;


import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ThreadDTO {

    public String id;
    public String threadType;     // DIRECT | GROUP
    public String name;           // human-readable thread name
    public String participantName;// backward-compat alias (DIRECT threads)
    public String lastMessage;
    public Instant lastMessageAt;
    public int unreadCount;
    public int participantCount;
    public List<ThreadParticipantDTO> participants; // populated in detail views
    public Instant createdAt;

    // Lead context — present when the thread is linked to a lead
    public String dealId;
    public String dealStatus;
    public String coupleWeddingDate;
    public String coupleLocation;

    // ── Factories ────────────────────────────────────────────────────────────

    public static ThreadDTO from(Thread t, UUID viewerUserId, long unread, Message lastMsg) {
        return from(t, viewerUserId, unread, lastMsg, null);
    }

    public static ThreadDTO from(Thread t, UUID viewerUserId, long unread,
                                  Message lastMsg,
                                  List<ThreadParticipantDTO> participants) {
        ThreadDTO dto = new ThreadDTO();
        dto.id           = t.id.toString();
        dto.createdAt    = t.createdAt;
        dto.lastMessageAt = t.lastMessageAt;
        dto.unreadCount  = (int) unread;
        dto.threadType   = t.threadType != null ? t.threadType.name() : "DIRECT";

        if (participants != null) {
            dto.participants    = participants;
            dto.participantCount = participants.size();
        }

        if (lastMsg != null) {
            dto.lastMessage = lastMsg.content;
        }

        if (t.threadType == ThreadType.GROUP && t.couple != null) {
            // Group thread: name is the couple's names
            dto.name            = t.couple.partner1Name + " & " + t.couple.partner2Name;
            dto.participantName = dto.name;

        } else if (t.lead != null) {
            // Direct thread linked to a lead
            dto.dealId     = t.lead.id.toString();
            dto.dealStatus = t.lead.status != null ? t.lead.status.name() : null;

            if (t.lead.couple != null) {
                dto.coupleWeddingDate = t.lead.couple.weddingDate != null
                        ? t.lead.couple.weddingDate.toString() : null;
                dto.coupleLocation = t.lead.couple.weddingLocation;
            }

            if (t.lead.couple != null && t.lead.couple.user != null
                    && t.lead.couple.user.id.equals(viewerUserId)) {
                dto.participantName = t.lead.vendor != null
                        ? t.lead.vendor.businessName : "Vendor";
            } else {
                dto.participantName = t.lead.couple != null
                        ? t.lead.couple.partner1Name + " & " + t.lead.couple.partner2Name
                        : "Couple";
            }
            dto.name = dto.participantName;

        } else {
            dto.participantName = "Unknown";
            dto.name            = "Unknown";
        }

        return dto;
    }
}
