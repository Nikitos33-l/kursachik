package com.example.payment.service.dto.yookassa.request;

public record YooKassaRequest(
        Amount amount,
        boolean capture,
        Confirmation confirmation,
        String description
) {
}
