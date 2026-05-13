package org.example.station.service.api.common.dto.response;

public record SummaryResponseStationDto(
        Long stationId,
        String name,
        String address
) {
}
