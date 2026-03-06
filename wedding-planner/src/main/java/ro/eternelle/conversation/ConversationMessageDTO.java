package ro.eternelle.conversation;

import java.time.Instant;
import java.util.UUID;

public class ConversationMessageDTO {
    public UUID id;
    public UUID conversationId;
    public UUID senderId;
    public String senderName;
    public ConversationMessage.SenderRole senderRole;
    public String content;
    public ConversationMessageType type;
    public Instant sentAt;

    public static ConversationMessageDTO from(ConversationMessage m) {
        ConversationMessageDTO dto = new ConversationMessageDTO();
        dto.id = m.id;
        dto.conversationId = m.conversation != null ? m.conversation.id : null;
        if (m.sender != null) {
            dto.senderId = m.sender.id;
            dto.senderName = m.sender.email;
        }
        dto.senderRole = m.senderRole;
        dto.content = m.content;
        dto.type = m.type != null ? m.type : ConversationMessageType.TEXT;
        dto.sentAt = m.sentAt;
        return dto;
    }
}
