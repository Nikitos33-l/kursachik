package org.example.notificationservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record Request(
        @Email
        String email,
        @NotNull
        Long orderId,
        @NotNull
        OrderNotificationType type
) {
}
