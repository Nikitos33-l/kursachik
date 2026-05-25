package org.example.station.service.producer;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StationEventProducer {
    private final RabbitTemplate rabbitTemplate;
    private final String stationExchange;
    private final String stationDeletedRoutingKey;
    private final String servicesUpdatedRoutingKey;

    public StationEventProducer(RabbitTemplate rabbitTemplate, @Value("${station.exchange.name}") String stationExchange, @Value("${station.delete.routing.key}") String stationDeletedRoutingKey,@Value("${station.services.updated.routing.key}") String servicesUpdatedRoutingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.stationExchange = stationExchange;
        this.stationDeletedRoutingKey = stationDeletedRoutingKey;
        this.servicesUpdatedRoutingKey = servicesUpdatedRoutingKey;
    }


    public void publishUserDeletedEvent(Long stationId){
        rabbitTemplate.convertAndSend(stationExchange,stationDeletedRoutingKey,stationId);
    }

    public void sendStationServicesUpdatedEvent(Long stationId) {
        rabbitTemplate.convertAndSend(stationExchange, servicesUpdatedRoutingKey, stationId);
    }

}
