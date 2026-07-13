package org.example.payment.service.dto.yookassa.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record YooKassaWebhookNotification(
        String type,
        String event,
        @JsonProperty("object") YooKassaWebhookObject paymentObject
) {
}
