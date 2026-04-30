package org.example.kursach.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ServiceRequestDto(
        @NotBlank
     String name,
    @NotNull
    double price
) { }
