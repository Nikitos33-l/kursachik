package org.example.user.contracts;

import java.util.UUID;

public record UserCreatedEvent(
        UUID id,
        String email,
        String password,
        String role,
        Long workplaceId
) {

}
