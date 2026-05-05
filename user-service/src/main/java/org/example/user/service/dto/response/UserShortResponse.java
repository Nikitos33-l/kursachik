package org.example.user.service.dto.response;

public record UserShortResponse(
        Long id,
        String name,
        String email
) {
}
