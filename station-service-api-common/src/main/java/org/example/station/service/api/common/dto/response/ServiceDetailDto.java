package org.example.station.service.api.common.dto.response;

import java.math.BigDecimal;

public record ServiceDetailDto(
        Long id,
        String name,
        BigDecimal price
) {}
