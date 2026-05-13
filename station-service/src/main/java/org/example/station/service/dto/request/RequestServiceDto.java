package org.example.station.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RequestServiceDto(
        @NotBlank
        String name,
        @NotNull
        BigDecimal price
) {
}
