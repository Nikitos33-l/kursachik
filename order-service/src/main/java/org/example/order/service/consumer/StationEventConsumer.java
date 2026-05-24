package org.example.order.service.consumer;

import lombok.RequiredArgsConstructor;
import org.example.order.service.service.OrderManagementService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StationEventConsumer {
    private final OrderManagementService service;

    @RabbitListener(queues = "${station.delete.queue}")
    public void handleStationDelete(@Payload Long stationId){
        service.deleteByStation(stationId);
    }

}
