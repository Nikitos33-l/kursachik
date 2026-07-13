package org.example.order.service.api.common.dto;

import java.math.BigDecimal;

public record OrderTotalResponse(
        BigDecimal total
) {
}
