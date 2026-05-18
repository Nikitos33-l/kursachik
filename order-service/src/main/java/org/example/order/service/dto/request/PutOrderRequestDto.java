package org.example.order.service.dto.request;

import java.util.Set;
import java.util.UUID;

public record PutOrderRequestDto(
        Set<UUID> workersId,
        String statusId
) {
}
