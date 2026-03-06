package ro.eternelle.review;

import java.time.Instant;
import java.util.UUID;

public class ReviewDTO {
    public UUID id;
    public UUID bookingId;
    public UUID coupleId;
    public String coupleName;
    public UUID vendorId;
    public double rating;
    public String comment;
    public String vendorReply;
    public boolean isPublic;
    public Instant createdAt;

    public static ReviewDTO from(Review r) {
        ReviewDTO dto = new ReviewDTO();
        dto.id = r.id;
        dto.bookingId = r.booking != null ? r.booking.id : null;
        dto.coupleId = r.couple != null ? r.couple.id : null;
        if (r.couple != null) {
            String p1 = r.couple.partner1Name != null ? r.couple.partner1Name : "";
            String p2 = r.couple.partner2Name != null ? r.couple.partner2Name : "";
            dto.coupleName = p2.isEmpty() ? p1 : p1 + " & " + p2;
        }
        dto.vendorId = r.vendor != null ? r.vendor.id : null;
        dto.rating = r.rating != null ? r.rating.doubleValue() : 0;
        dto.comment = r.comment;
        dto.vendorReply = r.vendorReply;
        dto.isPublic = r.isPublic;
        dto.createdAt = r.createdAt;
        return dto;
    }
}
