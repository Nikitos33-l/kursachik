package com.example.payment.service.dto;

import java.util.UUID;

public record PaymentResponse(
        UUID paymentId,
        String checkoutUrl
) {
}
