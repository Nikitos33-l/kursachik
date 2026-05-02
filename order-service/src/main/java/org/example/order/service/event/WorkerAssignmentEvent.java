package org.example.order.service.event;

public record WorkerAssignmentEvent(
        Long orderId,
        String workerEmail
) {
}
