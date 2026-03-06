package ro.eternelle.vendor.dto;

import java.util.List;

public record VendorOverviewDTO(
        RevenueStatDTO revenue,
        LeadsStatDTO leads,
        ViewsStatDTO profileViews,
        RatingStatDTO avgRating,
        ResponseRateDTO responseRate,
        List<MonthlyLeadDTO> monthlyLeads,
        List<RecentLeadDTO> recentLeads,
        List<UpcomingCallDTO> upcomingCalls,
        ProfilePerformanceDTO profilePerformance
) {}
