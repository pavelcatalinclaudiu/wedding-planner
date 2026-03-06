package ro.eternelle.lead;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class LeadDTO {
    public UUID id;
    public UUID coupleId;
    public String coupleName;
    public UUID vendorId;
    public String vendorName;
    public String vendorCategory;
    public LocalDate eventDate;
    public BigDecimal budget;
    public String message;
    public LeadStatus status;
    public Instant createdAt;
    public Instant updatedAt;

    public static LeadDTO from(Lead l) {
        LeadDTO dto = new LeadDTO();
        dto.id = l.id;
        if (l.couple != null) {
            dto.coupleId = l.couple.id;
            String p1 = l.couple.partner1Name != null ? l.couple.partner1Name : "";
            String p2 = l.couple.partner2Name != null ? l.couple.partner2Name : "";
            dto.coupleName = p2.isEmpty() ? p1 : p1 + " & " + p2;
        }
        if (l.vendor != null) {
            dto.vendorId = l.vendor.id;
            dto.vendorName = l.vendor.businessName;
            dto.vendorCategory = l.vendor.category != null ? l.vendor.category.name() : null;
        }
        dto.eventDate = l.eventDate;
        dto.budget = l.budget;
        dto.message = l.message;
        dto.status = l.status;
        dto.createdAt = l.createdAt;
        dto.updatedAt = l.updatedAt;
        return dto;
    }
}
