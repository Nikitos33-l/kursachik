package org.example.order.service.service;

import org.example.order.service.event.FailedStationOrdersDeletedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StationDeleteProcessorTest {

    @Mock private OutboxEventService eventService;
    @Mock private OrderManagementService orderManagementService;

    @InjectMocks private StationDeleteProcessor stationDeleteProcessor;

    private final String correlationId = UUID.randomUUID().toString();

    @Test
    @DisplayName("Успешное удаление заказов станции")
    public void deleteStationOrders_Success() {
        Long stationId = 1L;
        doNothing().when(orderManagementService).deleteByStation(stationId, correlationId);

        stationDeleteProcessor.deleteStationOrders(stationId, correlationId);

        verify(eventService, never()).saveFailedStationOrdersDeleted(any(), any());
        verify(orderManagementService, times(1)).deleteByStation(stationId, correlationId);
    }

    @Test
    @DisplayName("Выбрасывается исключение во время удаления, публикуется событие об ошибке")
    public void deleteStationOrders_Failure() {
        Long stationId = 1L;
        String errorMessage = "Database connection error";
        doThrow(new RuntimeException(errorMessage)).when(orderManagementService).deleteByStation(stationId, correlationId);

        stationDeleteProcessor.deleteStationOrders(stationId, correlationId);

        verify(orderManagementService, times(1)).deleteByStation(stationId, correlationId);

        ArgumentCaptor<FailedStationOrdersDeletedEvent> captor = ArgumentCaptor.forClass(FailedStationOrdersDeletedEvent.class);

        verify(eventService, times(1)).saveFailedStationOrdersDeleted(captor.capture(), eq(correlationId));

        FailedStationOrdersDeletedEvent event = captor.getValue();

        assertEquals(stationId, event.stationId());
        assertEquals(errorMessage, event.reason());
    }
}