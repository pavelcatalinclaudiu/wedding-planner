package ro.eternelle.offer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class OfferDTO {
    public UUID       id;
    public UUID       leadId;
    public UUID       conversationId;
    public UUID       vendorId;
    public String     vendorName;
    public String     packageDetails;
    public BigDecimal price;
    public LocalDate  expiresAt;
    public String     status;
    public Instant    viewedAt;
    public Instant    createdAt;

    public static OfferDTO from(Offer o) {
        OfferDTO dto = new OfferDTO();
        dto.id             = o.id;
        dto.leadId         = o.lead != null ? o.lead.id : null;
        dto.conversationId = o.conversation != null ? o.conversation.id : null;
        dto.vendorId       = o.vendor != null ? o.vendor.id : null;
        dto.vendorName     = o.vendor != null ? o.vendor.businessName : null;
        dto.packageDetails = o.packageDetails;
        dto.price          = o.price;
        dto.expiresAt      = o.expiresAt;
        dto.status         = o.status != null ? o.status.name() : null;
        dto.viewedAt       = o.viewedAt;
        dto.createdAt      = o.createdAt;
        return dto;
    }
}
