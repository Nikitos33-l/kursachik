package com.example.payment.service.service;

import com.example.order.service.api.common.OrderServiceFeignClient;
import com.example.order.service.api.common.dto.OrderTotalResponse;
import com.example.payment.service.dto.PaymentDetails;
import com.example.payment.service.dto.PaymentResponse;
import com.example.payment.service.dto.RemotePaymentResult;
import com.example.payment.service.entity.Payment;
import com.example.payment.service.entity.PaymentStatus;
import com.example.payment.service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {
    private final OrderServiceFeignClient orderServiceFeignClient;
    private final PaymentRepository paymentRepository;
    private final ExternalPaymentService externalPaymentService;

    @Transactional
    public PaymentResponse initiatePayment(Long orderId) {
        log.info("[Инициализация оплаты] Старт процесса для заказа №{}", orderId);

        Optional<Payment> activePayment = paymentRepository.findByPaymentStatusAndOrderId(PaymentStatus.PENDING, orderId);
        if (activePayment.isPresent()) {
            Payment existing = activePayment.get();
            log.info("[Инициализация оплаты] Обнаружен активный платеж PENDING для заказа №{}. " +
                    "Локальный ID платежа: {}. Возвращаем сохраненную ссылку.", orderId, existing.getId());
            return new PaymentResponse(existing.getId(), existing.getConfirmationUrl());
        }

        BigDecimal amount = fetchOrderTotal(orderId);
        Payment payment = createPendingPayment(orderId, amount);
        PaymentResponse response = registerInExternalSystem(payment, orderId);

        log.info("[Инициализация оплаты] Успешно завершено для заказа №{}. Локальный ID: {}, Ссылка выдана.",
                orderId, payment.getId());
        return response;
    }

    private BigDecimal fetchOrderTotal(Long orderId) {
        log.debug("[Feign] Запрос стоимости заказа №{} из order-service...", orderId);
        OrderTotalResponse orderTotal = orderServiceFeignClient.getTotalByOrderId(orderId);

        if (orderTotal == null || orderTotal.total() == null) {
            log.error("[Feign] Ошибка получения данных! order-service вернул пустой ответ (null) для заказа №{}", orderId);
            throw new IllegalArgumentException("Не удалось получить сумму заказа для orderId: " + orderId);
        }

        log.debug("[Feign] Успешно получена стоимость заказа №{}: {} BYN", orderId, orderTotal.total());
        return orderTotal.total();
    }

    private Payment createPendingPayment(Long orderId, BigDecimal amount) {
        log.debug("[БД] Создание черновика платежа для заказа №{} на сумму {} BYN", orderId, amount);
        Payment payment = Payment
                .builder()
                .orderId(orderId)
                .amount(amount)
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        log.debug("[БД] Локальный платеж успешно сохранен. Присвоен UUID: {}", savedPayment.getId());
        return savedPayment;
    }

    private PaymentResponse registerInExternalSystem(Payment payment, Long orderId) {
        log.info("[ЮKassa] Регистрация транзакции для локального платежа: {} (Заказ №{})", payment.getId(), orderId);

        String description = "Оплата заказа №" + orderId + " на СТО";
        PaymentDetails details = new PaymentDetails(
                payment.getId(),
                orderId,
                payment.getAmount(),
                description
        );

        RemotePaymentResult remoteResult = externalPaymentService.initiatePay(details);
        log.debug("[ЮKassa] Успешный ответ. Внешний ID платежа (ЮKassa): {}", remoteResult.externalId());

        payment.setExternalId(remoteResult.externalId());
        payment.setConfirmationUrl(remoteResult.checkoutUrl());
        paymentRepository.save(payment);
        log.debug("[БД] Локальный платеж {} обновлен внешними идентификаторами.", payment.getId());

        return new PaymentResponse(payment.getId(), remoteResult.checkoutUrl());
    }
}