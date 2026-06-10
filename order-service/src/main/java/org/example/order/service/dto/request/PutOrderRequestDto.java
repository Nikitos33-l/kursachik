package org.example.order.service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import java.util.UUID;

@Schema(description = "Объект для модификации заказа администратором (назначение исполнителей и статус)")
public record PutOrderRequestDto(
        @Schema(description = "Список UUID назначенных на заказ автомехаников", example = "[\"c3a34138-16dc-4431-b769-cf6f8bf9aa1a\"]")
        Set<UUID> workersId,

        @Schema(description = "Идентификатор устанавливаемого статуса заказа", example = "IN_PROGRESS")
        String statusId
) {
}