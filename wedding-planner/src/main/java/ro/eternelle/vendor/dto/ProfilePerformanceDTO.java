package ro.eternelle.vendor.dto;

public record ProfilePerformanceDTO(
        long searchRank,
        String searchRankCategory,
        double avgResponseTimeHours,
        int conversionRate,
        int platformAvgConversion
) {}
