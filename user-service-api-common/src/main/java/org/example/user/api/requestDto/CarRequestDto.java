package org.example.user.api.requestDto;

public record CarRequestDto(
        String make,
        String model,
        String number,
        Long ownerId
) {}
