package com.example.payment.service.dto.yookassa.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Confirmation(
        String type,
        @JsonProperty("return_url") String returnUrl
) {
}
