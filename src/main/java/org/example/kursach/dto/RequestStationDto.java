package org.example.kursach.dto;

import jakarta.validation.constraints.NotBlank;

public record RequestStationDto(
        @NotBlank
        String name,
        @NotBlank
        String address
) {
}
