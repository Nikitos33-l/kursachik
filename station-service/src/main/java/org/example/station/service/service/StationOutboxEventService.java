package org.example.station.service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.station.service.entity.OutboxEvent;
import org.example.station.service.entity.OutboxStatus;
import org.example.station.service.repository.OutboxEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StationOutboxEventService {
    private final OutboxEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    @Value("${station.exchange.name}")
    private String stationExchange;

    @Value("${station.delete.routing.key}")
    private String stationDeleteRoutingKey;

    @Value("${station.services.updated.routing.key}")
    private String servicesUpdatedRoutingKey;

    @Transactional(propagation = Propagation.REQUIRED)
    public void saveStationServicesUpdatedEvent(Long stationId) {
        log.debug("Формирование Outbox события обновления услуг для СТО ID: {}", stationId);
        saveEvent(stationId, servicesUpdatedRoutingKey);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void saveStationDeleteEvent(Long stationId) {
        log.debug("Формирование Outbox события удаления для СТО ID: {}", stationId);
        saveEvent(stationId, stationDeleteRoutingKey);
    }

    private void saveEvent(Long stationId, String routingKey) {
        try {
            String payloadJson = objectMapper.writeValueAsString(stationId);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(UUID.randomUUID())
                    .exchange(stationExchange)
                    .routingKey(routingKey)
                    .payload(payloadJson)
                    .status(OutboxStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();

            eventRepository.save(outboxEvent);
            log.info("Outbox событие с routingKey '{}' для СТО ID: {} успешно сохранено", routingKey, stationId);
        } catch (Exception e) {
            log.error("Критическая ошибка сериализации Outbox события для СТО ID: {} (RoutingKey: {})", stationId, routingKey, e);
            throw new RuntimeException("Не удалось сохранить событие во временную таблицу Outbox", e);
        }
    }
}
