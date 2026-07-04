package com.example.payment.service.dto.yookassa.responce;

public record YooKassaResponse(
        String id,
        String status,
        ConfirmationResponse confirmation
) {
}
