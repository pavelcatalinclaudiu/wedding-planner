package ro.eternelle.vendor.dto;

import java.time.Instant;

public record UpcomingCallDTO(
        String callId,
        String coupleName,
        Instant scheduledAt,
        String dealId
) {}
