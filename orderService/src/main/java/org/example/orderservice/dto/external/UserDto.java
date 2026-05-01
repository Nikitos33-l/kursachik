package org.example.orderservice.dto.external;


public record UserDto(
        Long id,
        String email,
        String name
) {
}
