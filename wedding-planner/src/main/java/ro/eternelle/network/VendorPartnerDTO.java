package ro.eternelle.network;

import java.time.Instant;
import java.util.UUID;

public class VendorPartnerDTO {

    public UUID id;

    /** Profile ID of the platform vendor — null for name-only entries */
    public String partnerId;

    /** Display name: businessName for platform vendors, custom text otherwise */
    public String partnerName;

    /** Category string, e.g. "PHOTOGRAPHER" — null for name-only entries */
    public String partnerCategory;

    /** City — null for name-only entries */
    public String partnerCity;

    /** Cover-photo URL — null for name-only entries */
    public String partnerCoverPhoto;

    /** true when the partner has a profile on the platform */
    public boolean onPlatform;

    public Instant createdAt;

    public static VendorPartnerDTO from(VendorPartner vp) {
        VendorPartnerDTO dto = new VendorPartnerDTO();
        dto.id        = vp.id;
        dto.createdAt = vp.createdAt;
        dto.onPlatform = vp.partner != null;

        if (vp.partner != null) {
            dto.partnerId       = vp.partner.id.toString();
            dto.partnerName     = vp.partner.businessName;
            dto.partnerCategory = vp.partner.category != null
                    ? vp.partner.category.name() : null;
            dto.partnerCity        = vp.partner.city;
            dto.partnerCoverPhoto  = vp.partner.coverPhoto;
        } else {
            dto.partnerName = vp.partnerName;
        }
        return dto;
    }
}
