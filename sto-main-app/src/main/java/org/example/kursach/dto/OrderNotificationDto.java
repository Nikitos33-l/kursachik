package org.example.kursach.dto;

public record OrderNotificationDto(
        Long orderId,
        String email,
        String type
) {
}
