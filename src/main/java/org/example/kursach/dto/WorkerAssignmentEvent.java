package org.example.kursach.dto;

public record WorkerAssignmentEvent(
        Long orderId,
        String workerEmail
) {
}
