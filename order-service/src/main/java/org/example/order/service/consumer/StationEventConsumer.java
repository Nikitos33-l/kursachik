package org.example.order.service.consumer;

import lombok.RequiredArgsConstructor;
import org.example.order.service.service.OrderManagementService;
import org.example.order.service.service.StationIntegrationWrapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class StationEventConsumer {
    private final OrderManagementService service;
    private final StationIntegrationWrapper stationIntegrationWrapper;

    @RabbitListener(queues = "${station.delete.queue}")
    public void handleStationDelete(@Payload Long stationId){
        service.deleteByStation(stationId);
        stationIntegrationWrapper.evictCache(stationId);
    }

    @RabbitListener(queues = "${station.services.updated.queue}")
    public void onStationServicesUpdated(Long stationId) {
       stationIntegrationWrapper.evictCache(stationId);
    }

}
