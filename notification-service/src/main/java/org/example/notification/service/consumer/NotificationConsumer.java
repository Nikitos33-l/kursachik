package org.example.notification.service.consumer;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notification.service.service.EmailPushService;
import org.example.notification.service.dto.Request;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Component
@RequiredArgsConstructor
@Validated
public class NotificationConsumer {
    private final EmailPushService service;

    @RabbitListener(queues = "${notification.queue}")
    public void sendMessage(
            @Valid @Payload Request request,
            @Header(AmqpHeaders.MESSAGE_ID) String messageId
    ) {
        log.info("[RABBITMQ CONSUMER] Получено событие из очереди уведомлений. Цель: '{}', Номер заказа: {}, ID сообщения: {}",
                request.email(), request.orderId(), messageId);
        try {
            service.sendMessage(request,messageId);
            log.debug("[RABBITMQ CONSUMER] Обработка таски для заказа ID: {} завершена без ошибок", request.orderId());
        } catch (Exception e) {
            log.error("[RABBITMQ CONSUMER] Не удалось обработать сообщение из очереди для заказа ID: {}. Ошибка перенаправлена в обработчик брокера.",
                    request.orderId());
            throw e;
        }
    }
}