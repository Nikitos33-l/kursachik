package com.example.payment.service.dto.yookassa.responce;

import com.example.payment.service.dto.yookassa.request.Amount;

public record YooKassaWebhookObject(
        String id,
        String status,
        Boolean paid,
        Amount amount,
        String description
) {
}
