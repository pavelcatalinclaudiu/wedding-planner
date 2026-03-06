package ro.eternelle.vendor.dto;

import java.util.List;

/**
 * Rich analytics for the Analytics dashboard — data the Overview never shows.
 * Every section focuses on historical depth, distributions, or trends.
 */
public record VendorDeepAnalyticsDTO(
        /** Actual booking revenue (from offer price) per month — last 12 months. */
        List<MonthlyRevenueDTO> revenueByMonth,
        /** How leads progress through the pipeline. */
        LeadFunnelDTO leadFunnel,
        /** Average rating bucketed by calendar quarter — last 4 quarters. */
        List<QuarterlyRatingDTO> ratingByQuarter,
        /** How many confirmed bookings fall in each calendar month (Jan–Dec). */
        List<BookingPeakDTO> bookingPeaks
) {

    public record MonthlyRevenueDTO(String month, int year, double revenue, long bookings) {}

    public record LeadFunnelDTO(
            long newLeads,
            long inDiscussion,
            long quoted,
            long booked,
            long declined
    ) {}

    public record QuarterlyRatingDTO(String label, double avgRating, int reviewCount) {}

    public record BookingPeakDTO(String month, int monthNumber, long count) {}
}
