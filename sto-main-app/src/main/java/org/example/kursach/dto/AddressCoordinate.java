package org.example.kursach.dto;

import java.math.BigDecimal;

public record AddressCoordinate(
        BigDecimal longitude,
        BigDecimal latitude
) {}
