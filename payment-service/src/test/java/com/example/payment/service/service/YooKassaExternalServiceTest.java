package com.example.payment.service.service;

import com.example.payment.service.dto.PaymentDetails;
import com.example.payment.service.dto.RemotePaymentResult;
import com.example.payment.service.dto.yookassa.responce.YooKassaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class YooKassaExternalServiceTest {

    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final Long ORDER_ID = 1L;
    private static final BigDecimal AMOUNT = new BigDecimal("150.00");
    private static final String DESCRIPTION = "Оплата заказа";
    private static final String EXTERNAL_ID = "yoo_out_987";
    private static final String CHECKOUT_URL = "https://yookassa.ru/checkout/link";

    @Mock(answer = Answers.RETURNS_SELF)
    private RestClient.Builder restClientBuilder;

    @Mock private RestClient restClient;
    @Mock private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock(answer = Answers.RETURNS_SELF)
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock private RestClient.ResponseSpec responseSpec;

    private YooKassaExternalService yooKassaExternalService;
    private PaymentDetails paymentDetails;

    @BeforeEach
    public void setUp() {
        lenient().doReturn(restClient).when(restClientBuilder).build();

        yooKassaExternalService = new YooKassaExternalService(
                restClientBuilder, "shop-id-123", "secret-key-456", "https://return.url"
        );

        paymentDetails = new PaymentDetails(PAYMENT_ID, ORDER_ID, AMOUNT, DESCRIPTION);
    }

    @Test
    @DisplayName("Успешное создание внешнего платежа в ЮKassa")
    public void initiatePaySuccess() {
        YooKassaResponse mockResponse = mock(YooKassaResponse.class, RETURNS_DEEP_STUBS);
        when(mockResponse.id()).thenReturn(EXTERNAL_ID);
        when(mockResponse.confirmation().confirmationUrl()).thenReturn(CHECKOUT_URL);

        mockRestClientChain();
        lenient().doReturn(mockResponse).when(responseSpec).body(YooKassaResponse.class);

        RemotePaymentResult result = yooKassaExternalService.initiatePay(paymentDetails);

        assertNotNull(result);
        assertEquals(EXTERNAL_ID, result.externalId());
        assertEquals(CHECKOUT_URL, result.checkoutUrl());
    }

    @Test
    @DisplayName("Выброс IllegalStateException, если ЮKassa вернула пустой ответ")
    public void initiatePayThrowsExceptionWhenResponseIsNull() {
        mockRestClientChain();
        lenient().doReturn(null).when(responseSpec).body(YooKassaResponse.class);

        assertThrows(IllegalStateException.class, () ->
                yooKassaExternalService.initiatePay(paymentDetails)
        );
    }

    @Test
    @DisplayName("Выброс IllegalStateException, если в ответе отсутствует блок confirmation")
    public void initiatePayThrowsExceptionWhenConfirmationIsNull() {
        YooKassaResponse mockResponse = mock(YooKassaResponse.class);
        when(mockResponse.confirmation()).thenReturn(null);

        mockRestClientChain();
        lenient().doReturn(mockResponse).when(responseSpec).body(YooKassaResponse.class);

        assertThrows(IllegalStateException.class, () ->
                yooKassaExternalService.initiatePay(paymentDetails)
        );
    }

    @Test
    @DisplayName("Проброс исключения при сетевом сбое HTTP-клиента")
    public void initiatePayPropagatesExceptionOnNetworkFailure() {
        mockRestClientChain();
        lenient().doThrow(new RuntimeException("Gateway Timeout")).when(responseSpec).body(YooKassaResponse.class);

        assertThrows(RuntimeException.class, () ->
                yooKassaExternalService.initiatePay(paymentDetails)
        );
    }

    private void mockRestClientChain() {
        lenient().doReturn(requestBodyUriSpec).when(restClient).post();
        lenient().doReturn(requestBodySpec).when(requestBodyUriSpec).uri(anyString());
        lenient().doReturn(responseSpec).when(requestBodySpec).retrieve();
    }
}