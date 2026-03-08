package ro.eternelle.vendor.dto;

import java.time.Instant;

public record RecentLeadDTO(
        String dealId,
        String coupleName,
        String coupleProfilePicture, // nullable
        String weddingDate,       // ISO date string, nullable
        String location,          // nullable
        String message,           // first message, truncated to 120 chars
        Instant receivedAt,
        String status
) {}
