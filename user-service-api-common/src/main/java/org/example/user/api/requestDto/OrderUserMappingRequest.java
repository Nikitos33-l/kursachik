package org.example.user.api.requestDto;

import java.util.Set;

public record OrderUserMappingRequest(
        Long orderId,
        Long userId,
        Set<Long> workersId,
        Long vehicleId
) {
}
