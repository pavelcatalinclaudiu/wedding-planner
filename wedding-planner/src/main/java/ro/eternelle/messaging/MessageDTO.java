package ro.eternelle.messaging;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class MessageDTO {

    public String id;
    public String threadId;
    public SenderDTO sender;
    public String content;
    public String type;
    public List<String> readBy;
    public Instant createdAt;

    public static class SenderDTO {
        public String id;
        public String email;
        public String role;
    }

    public static MessageDTO from(Message m) {
        MessageDTO dto = new MessageDTO();
        dto.id = m.id.toString();
        dto.threadId = m.thread != null ? m.thread.id.toString() : null;
        if (m.sender != null) {
            dto.sender = new SenderDTO();
            dto.sender.id = m.sender.id.toString();
            dto.sender.email = m.sender.email;
            dto.sender.role = m.sender.role != null ? m.sender.role.name() : null;
        }
        dto.content = m.content;
        dto.type = m.type != null ? m.type.name() : "TEXT";
        dto.readBy = m.readBy != null
                ? m.readBy.stream().map(UUID::toString).toList()
                : List.of();
        dto.createdAt = m.createdAt;
        return dto;
    }
}
