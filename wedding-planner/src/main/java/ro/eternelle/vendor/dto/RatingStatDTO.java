package ro.eternelle.vendor.dto;

public record RatingStatDTO(
        double value,
        int reviewCount,
        Long lastReviewDaysAgo   // null if no reviews yet
) {}
