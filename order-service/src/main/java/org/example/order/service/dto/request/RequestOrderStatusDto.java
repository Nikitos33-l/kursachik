package org.example.order.service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Объект для быстрой смены статуса заказа")
public record RequestOrderStatusDto(
        @Schema(description = "Уникальный строковый код нового статуса", example = "DONE")
        String id
) {
}