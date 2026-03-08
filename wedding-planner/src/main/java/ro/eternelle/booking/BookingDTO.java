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
    public String     vendorProfilePicture;
    public String     vendorCategory;
    public String     coupleName;
    public String     coupleProfilePicture;
    public String     status;
    public LocalDate  proposedDate;
    public String     proposedNote;

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
            dto.vendorName           = b.lead.vendor.businessName;
            dto.vendorProfilePicture = b.lead.vendor.profilePicture;
            dto.vendorCategory       = b.lead.vendor.category != null
                    ? b.lead.vendor.category.name()
                    : null;
        }
        if (b.lead != null && b.lead.couple != null) {
            String p1 = b.lead.couple.partner1Name != null ? b.lead.couple.partner1Name : "";
            String p2 = b.lead.couple.partner2Name != null ? b.lead.couple.partner2Name : "";
            dto.coupleName          = p2.isEmpty() ? p1 : p1 + " & " + p2;
            dto.coupleProfilePicture = b.lead.couple.profilePicture;
        }
        dto.status       = b.status != null ? b.status.name() : BookingStatus.CONFIRMED.name();
        dto.proposedDate = b.proposedDate;
        dto.proposedNote = b.proposedNote;
        return dto;
    }
}
