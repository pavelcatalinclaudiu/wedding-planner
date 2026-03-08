package ro.eternelle.vendor.dto;

import java.time.Instant;

public record UpcomingCallDTO(
        String callId,
        String coupleName,
        String coupleProfilePicture,
        Instant scheduledAt,
        String dealId
) {}
