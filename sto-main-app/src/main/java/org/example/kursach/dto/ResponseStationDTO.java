package org.example.kursach.dto;

import java.math.BigDecimal;

public record ResponseStationDTO(
        Long id,
        BigDecimal longitude,
        BigDecimal latitude,
        String name,
        String address
) {
}
