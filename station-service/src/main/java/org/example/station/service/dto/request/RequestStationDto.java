package org.example.station.service.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RequestStationDto(
        @NotBlank
        String name,
        @NotBlank
        String address
) {
}
