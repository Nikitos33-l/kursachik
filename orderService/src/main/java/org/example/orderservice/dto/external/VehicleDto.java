package org.example.orderservice.dto.external;

import lombok.Data;

public record VehicleDto(
        Long id,
    String make,
    String model,
    String number
)
{}
