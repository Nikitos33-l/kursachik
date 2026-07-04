package com.example.payment.service.service;

import com.example.payment.service.dto.PaymentDetails;
import com.example.payment.service.dto.RemotePaymentResult;
import com.example.payment.service.dto.yookassa.request.Amount;
import com.example.payment.service.dto.yookassa.request.Confirmation;
import com.example.payment.service.dto.yookassa.request.YooKassaRequest;
import com.example.payment.service.dto.yookassa.responce.YooKassaResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
public class YooKassaExternalService implements ExternalPaymentService {
    private final RestClient restClient;
    private final String returnUrl;

    public YooKassaExternalService(
            RestClient.Builder restClientBuilder,
            @Value("${yookassa.shop-id}") String shopId,
            @Value("${yookassa.secret-key}") String secretKey,
            @Value("${yookassa.return-url}") String returnUrl
    ) {
        this.returnUrl = returnUrl;

        String rawAuth = shopId + ":" + secretKey;
        String encodedAuth = Base64.getEncoder().encodeToString(rawAuth.getBytes());

        this.restClient = restClientBuilder
                .baseUrl("https://api.yookassa.ru/v3")
                .defaultHeader("Authorization", "Basic " + encodedAuth)
                .build();

        log.info("[ЮKassa API] HTTP-клиент успешно инициализирован для shopId: {}", shopId);
    }

    @Override
    public RemotePaymentResult initiatePay(PaymentDetails paymentDetails) {
        log.info("[ЮKassa API] Запрос на создание внешнего платежа для локального paymentId: {}", paymentDetails.paymentId());

        YooKassaRequest requestBody = buildRequest(paymentDetails);
        YooKassaResponse response = executeResponse(requestBody, paymentDetails.paymentId());

        if (response == null || response.confirmation() == null) {
            log.error("[ЮKassa API] Критическая ошибка: ЮKassa вернула пустой ответ или отсутствует блок confirmation для paymentId: {}",
                    paymentDetails.paymentId());
            throw new IllegalStateException("ЮKassa не вернула ссылку для подтверждения платежа");
        }

        log.info("[ЮKassa API] Платеж успешно зарегистрирован. Внешний ID ЮKassa: {}", response.id());
        return new RemotePaymentResult(response.id(), response.confirmation().confirmationUrl());
    }

    private YooKassaRequest buildRequest(PaymentDetails paymentDetails){
        log.debug("[ЮKassa API] Сборка JSON-запроса. Сумма: {} BYN, Назначение: '{}'",
                paymentDetails.amount().toPlainString(), paymentDetails.description());

        return new YooKassaRequest(
                new Amount(paymentDetails.amount().toPlainString(), "BYN"),
                true,
                new Confirmation("redirect", returnUrl),
                paymentDetails.description()
        );
    }

    private YooKassaResponse executeResponse(YooKassaRequest requestBody, UUID paymentId){
        log.debug("[ЮKassa API] Отправка POST /payments. Idempotence-Key (Локальный ID): {}", paymentId);

        try {
            YooKassaResponse response = restClient
                    .post()
                    .uri("/payments")
                    .header("Idempotence-Key", paymentId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(YooKassaResponse.class);

            log.debug("[ЮKassa API] Получен успешный HTTP-ответ от шлюза.");
            return response;

        } catch (Exception e) {
            log.error("[ЮKassa API] Сбой HTTP-запроса при обращении к эндпоинту ЮKassa для paymentId: {}. Причина: {}",
                    paymentId, e.getMessage(), e);
            throw e;
        }
    }
}