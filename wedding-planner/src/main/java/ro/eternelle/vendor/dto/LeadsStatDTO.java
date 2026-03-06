package ro.eternelle.vendor.dto;

public record LeadsStatDTO(
        long total,
        long thisMonth,
        long today,
        long trend
) {}
