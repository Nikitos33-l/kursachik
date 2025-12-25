package org.example.kursach.dto;

import jakarta.validation.constraints.NotNull;

public record ServiceRequestDto(
        @NotNull
     String name,
    @NotNull
    double price
) { }
