package org.example.security.service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.security.service.entity.OutboxEvent;
import org.example.security.service.entity.OutboxStatus;
import org.example.security.service.repository.OutboxEventRepository;
import org.example.user.contracts.UserCreatedEvent;
import org.example.user.contracts.UserRegisterEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserOutboxService {

    private final OutboxEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    @Value("${user.exchange.name}")
    private String exchange;

    @Value("${user.register.routing.key}")
    private String registerRoutingKey;

    @Transactional(propagation = Propagation.REQUIRED)
    public void saveRegisterEvent(UserRegisterEvent event) {
        saveEvent(registerRoutingKey, event);
    }

    private void saveEvent(String routingKey, Object payloadDto) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payloadDto);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(UUID.randomUUID())
                    .exchange(exchange)
                    .routingKey(routingKey)
                    .payload(jsonPayload)
                    .status(OutboxStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();

            eventRepository.save(outboxEvent);
            log.debug("Событие успешно сохранено в Outbox с ключом '{}'", routingKey);

        } catch (JsonProcessingException e) {
            log.error("Критическая ошибка сериализации события для Outbox. Routing key: {}", routingKey, e);
            throw new IllegalStateException("Не удалось сохранить событие в Outbox из-за ошибки JSON", e);
        }
    }
}