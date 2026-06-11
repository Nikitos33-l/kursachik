package org.example.gateway.dto;

import java.time.LocalDateTime;

public record FallbackResponse(
        String message,
        String reason,
        String timestamp
) {
    public FallbackResponse(String message, String reason) {
        this(message, reason, LocalDateTime.now().toString());
    }
}
