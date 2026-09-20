package org.example.station.service.consumer;

import lombok.RequiredArgsConstructor;
import org.example.station.service.service.StationDeleteSagaOrchestrator;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserEventConsumer {
    private final StationDeleteSagaOrchestrator stationDeleteSagaOrchestrator;

    @RabbitListener(queues = "${user.success.station.delete.queue}")
    public void handleUserSuccessStationDelete(@Payload Long stationId, @Header(value = AmqpHeaders.CORRELATION_ID) String correlationId){
        stationDeleteSagaOrchestrator.handleUserSuccessStationDelete(stationId,correlationId);
    }




}
