package ro.eternelle.vendor;

import java.util.UUID;

public class AvailabilityDTO {
    public UUID id;
    public UUID vendorId;
    public String date;   // "yyyy-MM-dd"
    public String reason;

    public static AvailabilityDTO from(VendorAvailability a) {
        AvailabilityDTO dto = new AvailabilityDTO();
        dto.id = a.id;
        dto.vendorId = a.vendor != null ? a.vendor.id : null;
        dto.date = a.date != null ? a.date.toString() : null;
        dto.reason = a.reason;
        return dto;
    }
}
