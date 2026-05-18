package org.example.user.service.dto.response;

import java.util.UUID;

public record ResponseUserDto(
        UUID id,
        String name,
        String email,
        String role
) {
}
