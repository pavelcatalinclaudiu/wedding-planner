package ro.eternelle.vendor.dto;

import java.math.BigDecimal;

public record RevenueStatDTO(
        BigDecimal current,
        BigDecimal trend,
        long bookingCount
) {}
