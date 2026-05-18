package org.example.user.api.requestDto;

import java.util.UUID;

public record CarRequestDto(
        String make,
        String model,
        String number,
        UUID ownerId
) {}
