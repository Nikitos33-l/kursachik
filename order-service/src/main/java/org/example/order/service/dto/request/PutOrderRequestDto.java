package org.example.order.service.dto.request;

import java.util.Set;

public record PutOrderRequestDto(
        Set<Long> workersId,
        String statusId
) {
}
