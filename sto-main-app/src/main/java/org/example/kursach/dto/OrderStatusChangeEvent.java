package org.example.kursach.dto;

import org.example.kursach.entity.OrderStatus;

public record OrderStatusChangeEvent(
        Long orderId,
        OrderStatus newStatus,
        String userEmail
) {

}
