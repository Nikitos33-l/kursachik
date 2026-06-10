package org.example.user.service.producer;

import lombok.extern.slf4j.Slf4j;
import org.example.user.contracts.UserCreatedEvent;
import org.example.user.contracts.UserUpdateEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class UserEventProducer {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String deleteRoutingKey;
    private final String addRoutingKey;
    private final String updateRoutingKey;

    public UserEventProducer(RabbitTemplate rabbitTemplate,
                             @Value("${user.exchange.name}") String exchange,
                             @Value("${user.delete.routing.key}") String deleteRoutingKey,
                             @Value("${user.create.routing.key}") String addRoutingKey,
                             @Value("${user.update.routing.key}") String updateRoutingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.deleteRoutingKey = deleteRoutingKey;
        this.addRoutingKey = addRoutingKey;
        this.updateRoutingKey = updateRoutingKey;
    }

    public void publishUserDeletedEvents(UUID userId) {
        log.info("Отправка события удаления пользователя. Брокер: [Exchange: '{}', RoutingKey: '{}']. User ID: {}",
                exchange, deleteRoutingKey, userId);
        rabbitTemplate.convertAndSend(exchange, deleteRoutingKey, userId);
    }

    public void publishUserCreatedEvents(UserCreatedEvent message) {
        log.info("Отправка события создания пользователя. Брокер: [Exchange: '{}', RoutingKey: '{}']. User ID: {}, Email: {}",
                exchange, addRoutingKey, message.id(), message.email());
        rabbitTemplate.convertAndSend(exchange, addRoutingKey, message);
    }

    public void publishUserUpdateEvents(UserUpdateEvent message) {
        log.info("Отправка события обновления пользователя. Брокер: [Exchange: '{}', RoutingKey: '{}']. User ID: {}, New Email: {}",
                exchange, updateRoutingKey, message.id(), message.email());
        rabbitTemplate.convertAndSend(exchange, updateRoutingKey, message);
    }
}