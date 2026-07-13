package org.example.payment.service.dto.yookassa.responce;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ConfirmationResponse(
        String type,
        @JsonProperty("confirmation_url") String confirmationUrl
) {
}
