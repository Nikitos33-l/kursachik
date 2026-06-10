package org.example.security.service.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.security.service.service.SecurityUserService;
import org.example.user.contracts.UserCreatedEvent;
import org.example.user.contracts.UserUpdateEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class UserEventConsumer {

    private final SecurityUserService userService;

    @RabbitListener(queues = "${security.queue.user-created}")
    public void handleUserCreated(@Payload UserCreatedEvent event) {
        log.info("[RABBITMQ CONSUMER] Получено событие 'USER_CREATED' для UUID: {}, Email: '{}'", event.id(), event.email());
        try {
            userService.createUser(event);
            log.debug("[RABBITMQ CONSUMER] Событие 'USER_CREATED' для UUID {} успешно обработано", event.id());
        } catch (Exception e) {
            log.error("[RABBITMQ CONSUMER] Ошибка при обработке 'USER_CREATED' для UUID {}: {}", event.id(), e.getMessage(), e);
            throw e;
        }
    }

    @RabbitListener(queues = "${security.queue.user-updated}")
    public void handleUserUpdated(@Payload UserUpdateEvent event) {
        log.info("[RABBITMQ CONSUMER] Получено событие 'USER_UPDATED' для UUID: {}, Новый Email: '{}'", event.id(), event.email());
        try {
            userService.updateUser(event);
            log.debug("[RABBITMQ CONSUMER] Событие 'USER_UPDATED' для UUID {} успешно обработано", event.id());
        } catch (Exception e) {
            log.error("[RABBITMQ CONSUMER] Ошибка при обработке 'USER_UPDATED' для UUID {}: {}", event.id(), e.getMessage(), e);
            throw e;
        }
    }

    @RabbitListener(queues = "${security.queue.user-deleted}")
    public void handleUserDeleted(@Payload UUID userId) {
        log.info("[RABBITMQ CONSUMER] Получено событие 'USER_DELETED' для UUID: {}", userId);
        try {
            userService.deleteUser(userId);
            log.debug("[RABBITMQ CONSUMER] Событие 'USER_DELETED' для UUID {} успешно обработано", userId);
        } catch (Exception e) {
            log.error("[RABBITMQ CONSUMER] Ошибка при обработке 'USER_DELETED' для UUID {}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }
}