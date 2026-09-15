package org.example.order.service.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.service.service.StationDeleteProcessor;
import org.example.order.service.service.StationIntegrationWrapper;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StationEventConsumer {
    private final StationIntegrationWrapper stationIntegrationWrapper;
    private final StationDeleteProcessor deleteProcessor;

    @RabbitListener(queues = "${station.delete.queue}")
    public void handleStationDelete(@Payload Long stationId,
                                    @Header(value = AmqpHeaders.CORRELATION_ID) String correlationId) {
        log.info("[RABBITMQ CONSUMER] Получено событие удаления СТО. ID: {}", stationId);
        deleteProcessor.deleteStationOrders(stationId,correlationId);
    }

    @RabbitListener(queues = "${station.services.updated.queue}")
    public void onStationServicesUpdated(Long stationId) {
        log.info("[RABBITMQ CONSUMER] Входящее событие: Обновлен прайс-лист услуг на СТО ID: {}. Сброс кэша валидации", stationId);
        stationIntegrationWrapper.evictCache(stationId);
    }
}