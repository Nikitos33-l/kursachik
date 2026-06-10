package org.example.station.service.producer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StationEventProducer {
    private final RabbitTemplate rabbitTemplate;
    private final String stationExchange;
    private final String stationDeletedRoutingKey;
    private final String servicesUpdatedRoutingKey;

    public StationEventProducer(RabbitTemplate rabbitTemplate,
                                @Value("${station.exchange.name}") String stationExchange,
                                @Value("${station.delete.routing.key}") String stationDeletedRoutingKey,
                                @Value("${station.services.updated.routing.key}") String servicesUpdatedRoutingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.stationExchange = stationExchange;
        this.stationDeletedRoutingKey = stationDeletedRoutingKey;
        this.servicesUpdatedRoutingKey = servicesUpdatedRoutingKey;
    }

    public void publishUserDeletedEvent(Long stationId) {
        log.info("[RABBITMQ] Публикация события удаления СТО. Station ID: {} -> Exchange: '{}', RoutingKey: '{}'",
                stationId, stationExchange, stationDeletedRoutingKey);

        rabbitTemplate.convertAndSend(stationExchange, stationDeletedRoutingKey, stationId);

        log.debug("[RABBITMQ] Событие удаления СТО ID: {} успешно передано в RabbitMQ", stationId);
    }

    public void sendStationServicesUpdatedEvent(Long stationId) {
        log.info("[RABBITMQ] Публикация события изменения прайс-листа услуг. Station ID: {} -> Exchange: '{}', RoutingKey: '{}'",
                stationId, stationExchange, servicesUpdatedRoutingKey);

        rabbitTemplate.convertAndSend(stationExchange, servicesUpdatedRoutingKey, stationId);

        log.debug("[RABBITMQ] Событие обновления услуг СТО ID: {} успешно передано в RabbitMQ", stationId);
    }
}