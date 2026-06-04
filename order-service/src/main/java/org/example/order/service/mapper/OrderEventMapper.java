package org.example.order.service.mapper;

import org.example.order.service.dto.OrderNotificationDto;
import org.example.order.service.event.OrderStatusChangeEvent;
import org.example.order.service.event.WorkerAssignmentEvent;
import org.springframework.stereotype.Component;

@Component
public class OrderEventMapper {

    public OrderNotificationDto toDto(WorkerAssignmentEvent event) {
        return new OrderNotificationDto(
                event.orderId(),
                event.workerEmail(),
                "NEW_ORDER_FOR_WORKER"
        );
    }

    public OrderNotificationDto toDto(OrderStatusChangeEvent event, String email) {
        String dbCode = event.newStatus().getId();

        String typeForBroker = switch (dbCode.toUpperCase()) {
            case "NEW"         -> "NEW";
            case "IN_PROGRESS" -> "IN_PROGRESS";
            case "DONE"        -> "READY";
            case "CANCELLED"   -> "CANCELLED";
            default            -> "NEW";
        };

        return new OrderNotificationDto(
                event.orderId(),
                email,
                typeForBroker
        );
    }
}