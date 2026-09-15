package org.example.user.service.service;

import org.example.user.contracts.UserStationFailedDeleteEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StationDeletionProcessorTest {

    @Mock
    private UserService userService;

    @Mock
    private UserOutboxService userOutboxService;

    @InjectMocks
    private StationDeletionProcessor stationDeletionProcessor;

    @Captor
    private ArgumentCaptor<UserStationFailedDeleteEvent> failedEventCaptor;

    @Test
    @DisplayName("Успешное удаление пользователей станции без записи ошибок в Outbox")
    void shouldSuccessfullyDeleteStationUsers() {
        Long stationId = 1L;
        String correlationId = UUID.randomUUID().toString();

        stationDeletionProcessor.deleteStationUsers(stationId, correlationId);

        verify(userService, times(1)).deleteByWorkplace(stationId, correlationId);
        verifyNoInteractions(userOutboxService);
    }

    @Test
    @DisplayName("При ошибке удаления пользователей сохраняется сбойное событие с верным ID станции, текстом ошибки и correlationId")
    void shouldSaveFailedEventToOutboxWhenDeletionFails() {
        Long stationId = 1L;
        String errorMessage = "Database connection timeout";
        String correlationId = UUID.randomUUID().toString();

        doThrow(new RuntimeException(errorMessage))
                .when(userService).deleteByWorkplace(stationId, correlationId);

        stationDeletionProcessor.deleteStationUsers(stationId, correlationId);

        verify(userService, times(1)).deleteByWorkplace(stationId, correlationId);

        verify(userOutboxService, times(1))
                .saveUserStationFailedDeleteEvent(failedEventCaptor.capture(), eq(correlationId));

        UserStationFailedDeleteEvent capturedEvent = failedEventCaptor.getValue();
        assertThat(capturedEvent.stationId()).isEqualTo(stationId);
        assertThat(capturedEvent.reason()).isEqualTo(errorMessage);
    }
}