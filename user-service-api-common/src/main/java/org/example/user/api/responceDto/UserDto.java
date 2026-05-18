package org.example.user.api.responceDto;


import java.util.UUID;

public record UserDto(
        UUID id,
        String email,
        String name
) {
}
