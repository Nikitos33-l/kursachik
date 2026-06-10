package org.example.order.service.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.service.service.OrderManagementService;
import org.example.order.service.service.UserIntegrationWrapper;
import org.example.user.contracts.UserUpdateEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventRabbitConsumer {
    private final OrderManagementService orderService;
    private final UserIntegrationWrapper userIntegrationWrapper;

    @RabbitListener(queues = "${user.delete.queue}")
    public void handleUserDelete(@Payload UUID userId) {
        log.info("[RABBITMQ CONSUMER] Получено событие удаления аккаунта пользователя UUID: {}", userId);
        try {
            orderService.deleteOrderByClient(userId);
            userIntegrationWrapper.evictCache(userId);
            log.info("[RABBITMQ CONSUMER] Каскадная очистка данных для пользователя UUID: {} завершена", userId);
        } catch (Exception e) {
            log.error("[RABBITMQ CONSUMER] Не удалось обработать удаление пользователя UUID: {}. Ошибка: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    @RabbitListener(queues = "${user.update.queue}")
    public void handleUpdateUser(@Payload UserUpdateEvent event) {
        log.info("[RABBITMQ CONSUMER] Получено событие изменения профиля пользователя UUID: {}. Сброс закэшированного email.", event.id());
        userIntegrationWrapper.evictCache(event.id());
    }
}