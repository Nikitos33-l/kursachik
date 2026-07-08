package com.example.payment.service.service;

import com.example.payment.service.entity.OutboxEvent;
import com.example.payment.service.entity.OutboxStatus;
import com.example.payment.service.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OutboxServiceTest {

    private static final Long ORDER_ID = 123L;
    private static final String JSON_PAYLOAD = "123";
    private static final String TEST_EXCHANGE = "order.exchange";
    private static final String TEST_ROUTING_KEY = "order.paid.routing.key";

    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private OutboxService outboxService;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(outboxService, "exchange", TEST_EXCHANGE);
        ReflectionTestUtils.setField(outboxService, "routingKey", TEST_ROUTING_KEY);
    }

    @Test
    @DisplayName("Успешное сохранение события в Outbox таблицу")
    public void savePaidEventSuccess() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(ORDER_ID)).thenReturn(JSON_PAYLOAD);

        outboxService.savePaidEvent(ORDER_ID);

        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, times(1)).save(eventCaptor.capture());

        OutboxEvent capturedEvent = eventCaptor.getValue();
        assertNotNull(capturedEvent);
        assertNotNull(capturedEvent.getEventId());
        assertEquals(TEST_EXCHANGE, capturedEvent.getExchange());
        assertEquals(TEST_ROUTING_KEY, capturedEvent.getRoutingKey());
        assertEquals(JSON_PAYLOAD, capturedEvent.getPayload());
        assertEquals(OutboxStatus.PENDING, capturedEvent.getStatus());
        assertNotNull(capturedEvent.getCreatedAt());

        verify(objectMapper, times(1)).writeValueAsString(ORDER_ID);
    }

    @Test
    @DisplayName("Выброс IllegalStateException при ошибке сериализации в JSON")
    public void savePaidEventThrowsExceptionWhenSerializationFails() throws JsonProcessingException {

        JsonProcessingException mockException = mock(JsonProcessingException.class);
        when(objectMapper.writeValueAsString(anyLong())).thenThrow(mockException);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                outboxService.savePaidEvent(ORDER_ID)
        );

        assertTrue(exception.getMessage().contains("Не удалось сохранить событие в Outbox"));
        assertEquals(mockException, exception.getCause());

        verifyNoInteractions(outboxEventRepository);
    }
}