package ro.eternelle.admin;

import ro.eternelle.user.User;
import ro.eternelle.couple.CoupleProfile;
import ro.eternelle.vendor.VendorProfile;

import java.time.Instant;

public class AdminUserDTO {

    public String id;
    public String email;
    public String role;
    public boolean emailVerified;
    public Instant createdAt;
    public Instant lastLogin;
    /** businessName for VENDORs, "Partner1 & Partner2" for COUPLEs, null for ADMIN */
    public String displayName;

    public static AdminUserDTO from(User u, CoupleProfile couple, VendorProfile vendor) {
        AdminUserDTO dto = new AdminUserDTO();
        dto.id = u.id.toString();
        dto.email = u.email;
        dto.role = u.role.name();
        dto.emailVerified = u.emailVerified;
        dto.createdAt = u.createdAt;
        dto.lastLogin = u.lastLogin;

        if (vendor != null) {
            dto.displayName = vendor.businessName;
        } else if (couple != null) {
            String p1 = couple.partner1Name != null ? couple.partner1Name : "";
            String p2 = couple.partner2Name != null ? couple.partner2Name : "";
            dto.displayName = p1.isEmpty() && p2.isEmpty() ? null
                    : p1.isBlank() ? p2 : p2.isBlank() ? p1 : p1 + " & " + p2;
        }

        return dto;
    }
}
