package com.example.payment.service.dto.yookassa.responce;

import com.fasterxml.jackson.annotation.JsonProperty;

public record YooKassaWebhookNotification(
        String type,
        String event,
        @JsonProperty("object") YooKassaWebhookObject paymentObject
) {
}
