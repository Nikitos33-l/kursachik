package org.example.user.contracts;

import java.util.UUID;

public record UserRegisterEvent(
        UUID id,
        String email,
        String name,
        String password,
        String role,
        Long workplaceId
) {
}
