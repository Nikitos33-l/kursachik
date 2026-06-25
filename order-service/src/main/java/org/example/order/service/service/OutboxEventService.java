package org.example.order.service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.service.dto.OrderNotificationDto;
import org.example.order.service.entity.OutboxEvent;
import org.example.order.service.entity.OutboxStatus;
import org.example.order.service.event.OrderStatusChangeEvent;
import org.example.order.service.event.WorkerAssignmentEvent;
import org.example.order.service.mapper.OrderEventMapper;
import org.example.order.service.repository.OutboxEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventService {

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final OrderEventMapper orderEventMapper;

    @Value("${notification.exchange}")
    private String exchange;

    @Value("${notification.routing.key}")
    private String routingKey;

    @Transactional(propagation = Propagation.REQUIRED)
    public void saveOrderStatusEvent(OrderStatusChangeEvent event) {
        saveSingleEvent(routingKey, orderEventMapper.toDto(event, event.userEmail()));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void saveWorkerAssignmentEvents(List<WorkerAssignmentEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        List<OutboxEvent> outboxEvents = events.stream()
                .map(event -> createOutboxEntity(routingKey, orderEventMapper.toDto(event)))
                .toList();

        outboxRepository.saveAll(outboxEvents);
        log.debug("Сохранено {} событий назначения мастеров в Outbox для уведомлений", events.size());
    }

    private void saveSingleEvent(String routingKey, OrderNotificationDto payloadDto) {
        outboxRepository.save(createOutboxEntity(routingKey, payloadDto));
        log.debug("Событие сохранено в Outbox. Exchange: '{}', Routing key: '{}'", exchange, routingKey);
    }

    private OutboxEvent createOutboxEntity(String routingKey, OrderNotificationDto payloadDto) {
        try {
            return OutboxEvent.builder()
                    .eventId(UUID.randomUUID())
                    .exchange(exchange)
                    .routingKey(routingKey)
                    .payload(objectMapper.writeValueAsString(payloadDto))
                    .status(OutboxStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();
        } catch (JsonProcessingException e) {
            log.error("Ошибка сериализации события для Outbox. Routing key: {}", routingKey, e);
            throw new IllegalStateException("Не удалось подготовить событие для Outbox", e);
        }
    }
}