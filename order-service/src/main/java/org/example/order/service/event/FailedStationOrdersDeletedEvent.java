package org.example.order.service.event;

public record FailedStationOrdersDeletedEvent(
        Long stationId,
        String reason
) {
}
