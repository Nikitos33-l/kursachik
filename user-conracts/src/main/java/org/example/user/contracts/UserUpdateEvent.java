package org.example.user.contracts;

import java.util.UUID;

public record UserUpdateEvent(
        UUID id,
        String email
) {
}
