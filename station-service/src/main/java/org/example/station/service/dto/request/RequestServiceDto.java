package org.example.station.service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Запрос на создание или обновление услуги автосервиса")
public record RequestServiceDto(
        @Schema(description = "Название оказываемой услуги", example = "Замена моторного масла", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String name,

        @Schema(description = "Стоимость услуги в базовой валюте", example = "85.50", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        BigDecimal price
) {
}