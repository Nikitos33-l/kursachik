package org.example.user.service.dto.response;

public record ResponseUserDto(
        Long id,
        String name,
        String email,
        String role
) {
}
