package org.example.order.service.event;


import org.example.order.service.entity.OrderStatus;

import java.util.UUID;

public record OrderStatusChangeEvent(
        Long orderId,
        OrderStatus newStatus,
        UUID userId,
        String userEmail
) {

}
