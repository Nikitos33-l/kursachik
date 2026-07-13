package org.example.payment.service.service;

import org.example.payment.service.entity.OutboxEvent;
import org.example.payment.service.entity.OutboxStatus;
import org.example.payment.service.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class OutboxService {
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Value("${order.exchange}")
    private String exchange;

    @Value("${order.paid.routing.key}")
    private String routingKey;


    @Transactional
    public void savePaidEvent(Long orderId){
        try {
            String jsonPayload = objectMapper.writeValueAsString(orderId);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(UUID.randomUUID())
                    .exchange(exchange)
                    .routingKey(routingKey)
                    .payload(jsonPayload)
                    .status(OutboxStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.info("Событие успешно сохранено в Outbox с ключом '{}'", routingKey);

        } catch (JsonProcessingException e) {
            log.error("Критическая ошибка сериализации события для Outbox. Routing key: {}", routingKey, e);
            throw new IllegalStateException("Не удалось сохранить событие в Outbox из-за ошибки JSON", e);
        }
    }
}
