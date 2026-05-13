package org.example.station.service.dto.response;

import java.math.BigDecimal;

public record ResponseStationDto(
        Long id,
        BigDecimal longitude,
        BigDecimal latitude,
        String name,
        String address
) {
}
