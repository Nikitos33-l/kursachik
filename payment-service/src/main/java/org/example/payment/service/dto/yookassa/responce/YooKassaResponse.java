package org.example.payment.service.dto.yookassa.responce;

public record YooKassaResponse(
        String id,
        String status,
        ConfirmationResponse confirmation
) {
}
