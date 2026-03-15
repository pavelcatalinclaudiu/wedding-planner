package ro.eternelle.admin;

import ro.eternelle.vendor.VendorProfile;

import java.math.BigDecimal;
import java.time.Instant;

public class AdminVendorDTO {

    public String id;
    public String userId;
    public String email;
    public String businessName;
    public String category;
    public String city;
    public BigDecimal basePrice;
    public double avgRating;
    public int reviewCount;
    public int profileViews;
    public boolean isVerified;
    public boolean isActive;
    public boolean monetizationEnabled;
    public long leadCount;
    public Instant createdAt;
    public String tier;

    public static AdminVendorDTO from(VendorProfile v, long leadCount) {
        AdminVendorDTO dto = new AdminVendorDTO();
        dto.id = v.id.toString();
        dto.userId = v.user.id.toString();
        dto.email = v.user.email;
        dto.businessName = v.businessName;
        dto.category = v.category != null ? v.category.name() : null;
        dto.city = v.city;
        dto.basePrice = v.basePrice;
        dto.avgRating = v.avgRating != null ? v.avgRating.doubleValue() : 0.0;
        dto.reviewCount = v.reviewCount;
        dto.profileViews = v.profileViews;
        dto.isVerified = v.isVerified;
        dto.isActive = v.isActive;
        dto.monetizationEnabled = v.monetizationEnabled;
        dto.leadCount = leadCount;
        dto.createdAt = v.createdAt;
        dto.tier = v.tier != null ? v.tier.name() : "FREE";
        return dto;
    }
}
