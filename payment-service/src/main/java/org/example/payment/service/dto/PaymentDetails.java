package org.example.payment.service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentDetails(
        UUID paymentId,
        Long orderId,
        BigDecimal amount,
        String description
) {}
