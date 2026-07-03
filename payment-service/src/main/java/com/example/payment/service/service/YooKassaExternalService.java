package com.example.payment.service.service;

import com.example.payment.service.dto.PaymentDetails;
import com.example.payment.service.dto.RemotePaymentResult;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Base64;

@Service
public class YooKassaExternalService implements ExternalPaymentService {
    private final RestClient restClient;


    public YooKassaExternalService(
            RestClient.Builder restClientBuilder,
            @Value("${yookassa.shop-id}") String shopId,
            @Value("${yookassa.secret-key}") String secretKey
    ) {

        String rawAuth = shopId + ":" + secretKey;

        String encodedAuth = Base64.getEncoder().encodeToString(rawAuth.getBytes());

        this.restClient = restClientBuilder
                .baseUrl("https://api.yookassa.ru/v3")
                .defaultHeader("Authorization", "Basic " + encodedAuth)
                .build();
    }

    @Override
    public RemotePaymentResult initiatePay(PaymentDetails paymentDetails) {
        //Todo написать метод для отправки запроса апи
        return null;
    }
}

