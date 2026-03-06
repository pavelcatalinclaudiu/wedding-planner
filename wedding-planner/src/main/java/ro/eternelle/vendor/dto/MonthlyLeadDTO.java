package ro.eternelle.vendor.dto;

public record MonthlyLeadDTO(
        String month,
        long count,
        boolean isCurrent
) {}
