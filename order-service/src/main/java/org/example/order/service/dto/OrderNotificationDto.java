package org.example.order.service.dto;

public record OrderNotificationDto(
        Long orderId,
        String email,
        String type
) {
}
