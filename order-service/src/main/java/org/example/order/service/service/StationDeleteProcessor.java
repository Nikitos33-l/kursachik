package org.example.order.service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.service.event.FailedStationOrdersDeletedEvent;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StationDeleteProcessor {
    private final OutboxEventService eventService;
    private final OrderManagementService orderManagementService;

    public void deleteStationOrders(Long stationId) {
        log.info("Запуск процесса удаления заказов для автостанции ID: {}", stationId);

        try {
            orderManagementService.deleteByStation(stationId);
            log.info("Заказы автостанции ID: {} успешно удалены", stationId);
        } catch (Exception e) {
            log.error("Сбой при удалении заказов автостанции ID: {}. Формируем событие ошибки в Outbox", stationId, e);

            FailedStationOrdersDeletedEvent event = new FailedStationOrdersDeletedEvent(stationId, e.getMessage());
            eventService.saveFailedStationOrdersDeleted(event);
        }
    }
}
