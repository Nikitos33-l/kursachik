package org.example.station.service.dto.response;

import java.math.BigDecimal;

public record ResponseServiceDto(
        Long id,
        String name,
        BigDecimal price
) {
}
