package org.example.order.service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.service.entity.OutboxEvent;
import org.example.order.service.entity.OutboxStatus;
import org.example.order.service.event.FailedStationOrdersDeletedEvent;
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
    private String notificationExchange;

    @Value("${order.exchange}")
    private String orderExchange;

    @Value("${notification.routing.key}")
    private String notificationRoutingKey;

    @Value("${order.success.station.delete.routing.key}")
    private String successStationOrdersDeletedKey;

    @Value("${order.failed.station.delete.routing.key}")
    private String failedStationOrdersDeletedKey;

    @Transactional(propagation = Propagation.REQUIRED)
    public void saveSuccessStationOrdersDeleted(Long stationId,String correlationId){
        saveSingleEvent(successStationOrdersDeletedKey,stationId,orderExchange,correlationId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailedStationOrdersDeleted(FailedStationOrdersDeletedEvent event,String correlationId){
        saveSingleEvent(failedStationOrdersDeletedKey,event,orderExchange,correlationId);
    }


    @Transactional(propagation = Propagation.REQUIRED)
    public void saveOrderStatusEvent(OrderStatusChangeEvent event) {
        saveSingleEvent(notificationRoutingKey, orderEventMapper.toDto(event, event.userEmail()),notificationExchange,null);
    }

    private void saveSingleEvent(String routingKey, Object payloadDto,String exchange,String correlationId) {
        outboxRepository.save(createOutboxEntity(routingKey, payloadDto,exchange,correlationId));
        log.debug("Событие сохранено в Outbox. Exchange: '{}', Routing key: '{}'", exchange, routingKey);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void saveWorkerAssignmentEvents(List<WorkerAssignmentEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        List<OutboxEvent> outboxEvents = events.stream()
                .map(event -> createOutboxEntity(notificationRoutingKey, orderEventMapper.toDto(event),notificationExchange,null))
                .toList();

        outboxRepository.saveAll(outboxEvents);
        log.debug("Сохранено {} событий назначения мастеров в Outbox для уведомлений", events.size());
    }


    private OutboxEvent createOutboxEntity(String routingKey, Object payloadDto,String exchange,String correlationId) {
        try {
            return OutboxEvent.builder()
                    .eventId(UUID.randomUUID())
                    .exchange(exchange)
                    .routingKey(routingKey)
                    .payload(objectMapper.writeValueAsString(payloadDto))
                    .status(OutboxStatus.PENDING)
                    .correlationId(correlationId)
                    .createdAt(LocalDateTime.now())
                    .build();
        } catch (JsonProcessingException e) {
            log.error("Ошибка сериализации события для Outbox. Routing key: {}", routingKey, e);
            throw new IllegalStateException("Не удалось подготовить событие для Outbox", e);
        }
    }
}