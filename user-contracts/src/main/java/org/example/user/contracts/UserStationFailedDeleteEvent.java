package org.example.user.contracts;

public record UserStationFailedDeleteEvent(
        Long stationId,
        String reason
) {
}
