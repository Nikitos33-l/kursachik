package org.example.order.service.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.service.service.OrderManagementService;
import org.example.order.service.service.StationIntegrationWrapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StationEventConsumer {
    private final OrderManagementService service;
    private final StationIntegrationWrapper stationIntegrationWrapper;

    @RabbitListener(queues = "${station.delete.queue}")
    public void handleStationDelete(@Payload Long stationId) {
        log.info("[RABBITMQ CONSUMER] Получено событие удаления СТО. ID: {}", stationId);
        try {
            service.deleteByStation(stationId);
            stationIntegrationWrapper.evictCache(stationId);
            log.info("[RABBITMQ CONSUMER] Все данные по СТО ID: {} успешно вычищены", stationId);
        } catch (Exception e) {
            log.error("[RABBITMQ CONSUMER] Критический сбой при обработке удаления СТО ID: {}. Ошибка: {}", stationId, e.getMessage(), e);
            throw e;
        }
    }

    @RabbitListener(queues = "${station.services.updated.queue}")
    public void onStationServicesUpdated(Long stationId) {
        log.info("[RABBITMQ CONSUMER] Входящее событие: Обновлен прайс-лист услуг на СТО ID: {}. Сброс кэша валидации", stationId);
        stationIntegrationWrapper.evictCache(stationId);
    }
}