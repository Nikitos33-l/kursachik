package org.example.station.service.dto;

import java.math.BigDecimal;

public record AddressCoordinate(
        BigDecimal longitude,
        BigDecimal latitude
) {
}
