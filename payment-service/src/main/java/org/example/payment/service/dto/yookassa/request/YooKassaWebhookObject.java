package org.example.payment.service.dto.yookassa.request;

public record YooKassaWebhookObject(
        String id,
        String status,
        Boolean paid,
        Amount amount,
        String description
) {
}
