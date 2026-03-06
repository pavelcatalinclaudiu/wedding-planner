package ro.eternelle.booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class BookingDTO {
    public UUID       id;
    public UUID       leadId;
    public UUID       offerId;
    public LocalDate  weddingDate;
    public BigDecimal totalPrice;
    public BigDecimal depositPaid;
    public Instant    depositPaidAt;
    public LocalDate  balanceDueAt;
    public String     contractUrl;
    public String     notes;
    public Instant    createdAt;
    public boolean    hasReview;
    public String     vendorName;
    public String     vendorCategory;

    public static BookingDTO from(Booking b, boolean hasReview) {
        BookingDTO dto = new BookingDTO();
        dto.id           = b.id;
        dto.leadId        = b.lead  != null ? b.lead.id  : null;
        dto.offerId       = b.offer != null ? b.offer.id : null;
        dto.weddingDate   = b.weddingDate;
        dto.totalPrice    = b.totalPrice;
        dto.depositPaid   = b.depositPaid;
        dto.depositPaidAt = b.depositPaidAt;
        dto.balanceDueAt  = b.balanceDueAt;
        dto.contractUrl   = b.contractUrl;
        dto.notes         = b.notes;
        dto.createdAt     = b.createdAt;
        dto.hasReview     = hasReview;
        if (b.lead != null && b.lead.vendor != null) {
            dto.vendorName     = b.lead.vendor.businessName;
            dto.vendorCategory = b.lead.vendor.category != null
                    ? b.lead.vendor.category.name()
                    : null;
        }
        return dto;
    }
}
