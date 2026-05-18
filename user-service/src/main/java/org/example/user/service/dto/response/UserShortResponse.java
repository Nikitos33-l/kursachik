package org.example.user.service.dto.response;

import java.util.UUID;

public record UserShortResponse(
        UUID id,
        String name,
        String email
) {
}
