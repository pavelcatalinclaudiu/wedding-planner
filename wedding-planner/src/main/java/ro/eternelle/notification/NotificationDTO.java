package ro.eternelle.notification;

import java.time.Instant;
import java.util.UUID;

public class NotificationDTO {

    public UUID    id;
    public String  type;
    public String  title;
    public String  body;
    public String  entityId;
    public boolean read;
    public Instant createdAt;

    public static NotificationDTO from(Notification n) {
        NotificationDTO dto = new NotificationDTO();
        dto.id        = n.id;
        dto.type      = n.type;
        dto.title     = n.title;
        dto.body      = n.body;
        dto.entityId  = n.entityId;
        dto.read      = n.read;
        dto.createdAt = n.createdAt;
        return dto;
    }
}
