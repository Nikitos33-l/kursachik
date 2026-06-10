package org.example.order.service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Информация об автомобиле клиента")
public record RequestVehicleDto(
        @Schema(description = "Марка автомобиля", example = "Volkswagen")
        String make,

        @Schema(description = "Модель автомобиля", example = "Golf GTI")
        String model,

        @Schema(description = "Государственный регистрационный знак (номер)", example = "7777 AB-7")
        String number
) {
}