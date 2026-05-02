package org.example.order.service.event;


import org.example.order.service.entity.OrderStatus;

public record OrderStatusChangeEvent(
        Long orderId,
        OrderStatus newStatus,
        String userEmail
) {

}
