package org.example.order.service.dto.response;

import java.math.BigDecimal;

public record OrderItemDto(
    Long id,
    String name,
    BigDecimal price
)
{}
