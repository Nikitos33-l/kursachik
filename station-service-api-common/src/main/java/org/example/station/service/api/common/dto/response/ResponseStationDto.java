package org.example.station.service.api.common.dto.response;

public record ResponseStationDto(
        Long stationId,
        String name,
        String address
) {
}
