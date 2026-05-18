package org.example.user.api.requestDto;

import java.util.Set;
import java.util.UUID;

public record OrderUserMappingRequest(
        Long orderId,
        UUID userId,
        Set<UUID> workersId,
        Long vehicleId
) {
}
