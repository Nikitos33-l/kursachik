package org.example.order.service.mapper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.order.service.dto.OrderNotificationDto;
import org.example.order.service.event.OrderStatusChangeEvent;
import org.example.order.service.event.WorkerAssignmentEvent;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class OrderEventMapper {

    public OrderNotificationDto toDto(WorkerAssignmentEvent event) {
        return new OrderNotificationDto(
                event.orderId(),
                event.workerEmail(),
                "NEW_ORDER_FOR_WORKER"
        );
    }

    public OrderNotificationDto toDto(OrderStatusChangeEvent event) {
        String dbCode = event.newStatus().getId();

        String typeForBroker = StatusMapping.getNotificationTypeByDbCode(dbCode);

        return new OrderNotificationDto(
                event.orderId(),
                event.userEmail(),
                typeForBroker
        );
    }

    @Getter
    @RequiredArgsConstructor
    private enum StatusMapping {
        NEW("NEW", "NEW"),
        IN_PROGRESS("IN_PROGRESS", "IN_PROGRESS"),
        DONE("DONE", "READY"),
        CANCELLED("CANCELLED", "CANCELLED");

        private final String dbCode;
        private final String notificationType;

        public static String getNotificationTypeByDbCode(String code) {
            return Arrays.stream(values())
                    .filter(s -> s.dbCode.equalsIgnoreCase(code))
                    .findFirst()
                    .map(StatusMapping::getNotificationType)
                    .orElse("NEW");
        }
    }
}