package org.example.security.service.api.common.dto;

import java.util.List;
import java.util.UUID;

public record TokenValidationResultDto(
        boolean isValid,
        UUID userId,
        String email,
        List<String> roles,
        Long stationId
) {
    public static TokenValidationResultDto invalid() {
        return new TokenValidationResultDto(false, null, null, null, null);
    }
}
