package ro.eternelle.admin;

import ro.eternelle.review.Review;

import java.time.Instant;

public class AdminReviewDTO {

    public String id;
    public double rating;
    public String comment;
    public String vendorReply;
    public boolean isPublic;
    public String status;
    public Instant createdAt;
    public String vendorId;
    public String vendorName;
    public String coupleId;
    public String coupleName;

    public static AdminReviewDTO from(Review r) {
        AdminReviewDTO dto = new AdminReviewDTO();
        dto.id = r.id.toString();
        dto.rating = r.rating != null ? r.rating.doubleValue() : 0.0;
        dto.comment = r.comment;
        dto.vendorReply = r.vendorReply;
        dto.isPublic = r.isPublic;
        dto.status = r.status;
        dto.createdAt = r.createdAt;

        if (r.vendor != null) {
            dto.vendorId = r.vendor.id.toString();
            dto.vendorName = r.vendor.businessName;
        }
        if (r.couple != null) {
            dto.coupleId = r.couple.id.toString();
            String p1 = r.couple.partner1Name != null ? r.couple.partner1Name : "";
            String p2 = r.couple.partner2Name != null ? r.couple.partner2Name : "";
            dto.coupleName = p1.isBlank() ? p2 : p2.isBlank() ? p1 : p1 + " & " + p2;
        }

        return dto;
    }
}
