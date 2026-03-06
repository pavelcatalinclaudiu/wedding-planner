package ro.eternelle.videocall;

import java.time.Instant;

public class VideoCallDTO {

    public String  id;
    public String  leadId;
    public String  roomUrl;
    public String  roomName;
    public String  status;
    public String  postCallAction;
    public Instant scheduledAt;
    public Instant startedAt;
    public Instant endedAt;
    public Instant createdAt;
    /** Caller-specific JWT for JaaS (null when JaaS not configured). */
    public String  token;
    public String  coupleName;
    public String  vendorName;
    public String  vendorId;
    /** "COUPLE" or "VENDOR" – whoever proposed / last rescheduled this call. */
    public String  proposedBy;

    public static VideoCallDTO from(VideoCall c) {
        return from(c, null);
    }

    public static VideoCallDTO from(VideoCall c, String token) {
        VideoCallDTO dto = new VideoCallDTO();
        dto.id             = c.id != null ? c.id.toString() : null;
        dto.leadId         = c.lead != null ? c.lead.id.toString() : null;
        dto.roomUrl        = c.roomUrl;
        dto.roomName       = c.roomName;
        dto.status         = c.status != null ? c.status.name() : null;
        dto.postCallAction = c.postCallAction != null ? c.postCallAction.name() : null;
        dto.scheduledAt    = c.scheduledAt;
        dto.startedAt      = c.startedAt;
        dto.endedAt        = c.endedAt;
        dto.createdAt      = c.createdAt;
        dto.token          = token;
        dto.proposedBy    = c.proposedBy;
        if (c.lead != null) {
            if (c.lead.couple != null) {
                dto.coupleName = c.lead.couple.partner1Name
                        + (c.lead.couple.partner2Name != null ? " & " + c.lead.couple.partner2Name : "");
            }
            if (c.lead.vendor != null) {
                dto.vendorName = c.lead.vendor.businessName;
                dto.vendorId = c.lead.vendor.id.toString();
            }
        }
        return dto;
    }
}
