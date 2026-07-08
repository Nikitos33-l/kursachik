package com.example.payment.service.service;

import com.example.order.service.api.common.OrderServiceFeignClient;
import com.example.order.service.api.common.dto.OrderTotalResponse;
import com.example.payment.service.dto.PaymentDetails;
import com.example.payment.service.dto.PaymentResponse;
import com.example.payment.service.dto.RemotePaymentResult;
import com.example.payment.service.entity.Payment;
import com.example.payment.service.entity.PaymentStatus;
import com.example.payment.service.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    private static final Long ORDER_ID = 1L;
    private static final BigDecimal AMOUNT = new BigDecimal("100.00");
    private static final String CHECKOUT_URL = "https://yoomoney.ru/checkout/example";
    private static final String EXTERNAL_ID = "yoo_external_id_123";

    @Mock private OrderServiceFeignClient orderServiceFeignClient;
    @Mock private PaymentRepository paymentRepository;
    @Mock private ExternalPaymentService externalPaymentService;

    @InjectMocks private PaymentService paymentService;

    @Test
    @DisplayName("Успешная инициализация оплаты")
    public void initiatePaymentSuccess() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = buildTestPayment(paymentId);

        when(paymentRepository.findByPaymentStatusAndOrderId(PaymentStatus.PENDING, ORDER_ID)).thenReturn(Optional.empty());
        when(orderServiceFeignClient.getTotalByOrderId(ORDER_ID)).thenReturn(new OrderTotalResponse(AMOUNT));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(externalPaymentService.initiatePay(any(PaymentDetails.class))).thenReturn(new RemotePaymentResult(EXTERNAL_ID, CHECKOUT_URL));

        PaymentResponse response = paymentService.initiatePayment(ORDER_ID);

        assertNotNull(response);
        assertEquals(paymentId, response.paymentId());
        assertEquals(CHECKOUT_URL, response.checkoutUrl());

        verify(paymentRepository, times(2)).save(any(Payment.class));
        verify(externalPaymentService, times(1)).initiatePay(any(PaymentDetails.class));
    }

    @Test
    @DisplayName("Возврат существующей ссылки при повторном клике")
    public void initiatePaymentReturnsExistingWhenPending() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = buildTestPayment(paymentId);

        when(paymentRepository.findByPaymentStatusAndOrderId(PaymentStatus.PENDING, ORDER_ID)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.initiatePayment(ORDER_ID);

        assertNotNull(response);
        assertEquals(paymentId, response.paymentId());
        assertEquals(CHECKOUT_URL, response.checkoutUrl());

        verifyNoInteractions(orderServiceFeignClient);
        verifyNoInteractions(externalPaymentService);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Выброс исключения, если сумма заказа пришла пустой")
    public void initiatePaymentThrowsExceptionWhenTotalIsNull() {
        when(paymentRepository.findByPaymentStatusAndOrderId(PaymentStatus.PENDING, ORDER_ID)).thenReturn(Optional.empty());
        when(orderServiceFeignClient.getTotalByOrderId(ORDER_ID)).thenReturn(new OrderTotalResponse(null));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                paymentService.initiatePayment(ORDER_ID)
        );

        assertTrue(exception.getMessage().contains("Не удалось получить сумму заказа"));
        verify(paymentRepository, never()).save(any(Payment.class));
        verifyNoInteractions(externalPaymentService);
    }

    private Payment buildTestPayment(UUID id) {
        return Payment.builder()
                .id(id)
                .orderId(ORDER_ID)
                .amount(AMOUNT)
                .confirmationUrl(CHECKOUT_URL)
                .paymentStatus(PaymentStatus.PENDING)
                .build();
    }
}