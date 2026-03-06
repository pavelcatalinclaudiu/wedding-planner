package ro.eternelle.conversation;

import java.time.Instant;
import java.util.UUID;

public class ConversationDTO {
    public UUID id;
    public UUID leadId;
    public UUID coupleId;
    public String coupleName;
    public UUID vendorId;
    public String vendorName;
    public Instant createdAt;
    public Instant lastMessageAt;

    public static ConversationDTO from(Conversation c) {
        ConversationDTO dto = new ConversationDTO();
        dto.id = c.id;
        dto.leadId = c.lead != null ? c.lead.id : null;
        if (c.couple != null) {
            dto.coupleId = c.couple.id;
            String p1 = c.couple.partner1Name != null ? c.couple.partner1Name : "";
            String p2 = c.couple.partner2Name != null ? c.couple.partner2Name : "";
            dto.coupleName = p2.isEmpty() ? p1 : p1 + " & " + p2;
        }
        if (c.vendor != null) {
            dto.vendorId = c.vendor.id;
            dto.vendorName = c.vendor.businessName;
        }
        dto.createdAt = c.createdAt;
        dto.lastMessageAt = c.lastMessageAt;
        return dto;
    }
}
