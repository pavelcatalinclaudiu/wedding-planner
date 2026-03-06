package ro.eternelle.vendor.dto;

public record ResponseRateDTO(
        int percent,
        int trend,
        long repliedCount,
        long totalEnquiries
) {}
