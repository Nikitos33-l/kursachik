package org.example.user.service.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.user.service.service.StationDeletionProcessor;
import org.example.user.service.service.UserService;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StationEventConsumer {
    private final StationDeletionProcessor deletionProcessor;

    @RabbitListener(queues = "${station.delete.queue}")
    public void handleDeleteStation(@Payload Long stationId,
                                    @Header(value = AmqpHeaders.CORRELATION_ID) String correlationId) {
        log.info("Вычитано событие удаления СТО из очереди. ID станции: {}", stationId);
        deletionProcessor.deleteStationUsers(stationId);
    }
}