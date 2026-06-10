package org.example.station.service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Запрос на добавление или редактирование филиала СТО")
public record RequestStationDto(
        @Schema(description = "Фирменное наименование станции", example = "СТО АвтоМастер-Запад", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String name,

        @Schema(description = "Полный физический адрес филиала", example = "г. Минск, ул. Сурганова, 47", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String address
) {
}