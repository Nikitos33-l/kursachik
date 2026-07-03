package com.example.payment.service.dto;

public record RemotePaymentResult(
        String externalId,
        String checkoutUrl
) {}
