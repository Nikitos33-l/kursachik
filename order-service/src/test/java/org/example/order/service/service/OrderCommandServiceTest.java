package org.example.order.service.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.order.service.entity.Order;
import org.example.order.service.entity.OrderStatus;
import org.example.order.service.event.OrderStatusChangeEvent;
import org.example.order.service.event.WorkerAssignmentEvent;
import org.example.order.service.repository.OrderRepository;
import org.example.order.service.repository.OrderStatusRepository;
import org.example.securitycommon.UserPrincipal;
import org.example.station.service.api.common.dto.response.ServiceDetailDto;
import org.example.user.api.responceDto.ValidationResponse;
import org.example.user.api.responceDto.VehicleDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderCommandServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderStatusRepository orderStatusRepository;
    @Mock private OutboxEventService outboxEventService;

    @InjectMocks
    private OrderCommandService orderCommandService;

    private final UUID clientId = UUID.randomUUID();
    private final Long orderId = 123L;

    @Test
    @DisplayName("saveNewOrder: Успешное сохранение заказа и создание Outbox события")
    void saveNewOrder_Success() {
        // Given
        UserPrincipal principal = new UserPrincipal(clientId, "client@test.com", null, List.of());
        VehicleDto vehicle = new VehicleDto(1L, "Audi", "A6", "1111-AA-7");
        List<ServiceDetailDto> services = List.of(
                new ServiceDetailDto(10L, "Замена колодок", new BigDecimal("150.00"))
        );

        OrderStatus statusNew = new OrderStatus();
        statusNew.setId("NEW");

        Order savedOrder = new Order();
        savedOrder.setId(orderId);
        savedOrder.setStatus(statusNew);
        savedOrder.setClientId(clientId);

        when(orderStatusRepository.findById("NEW")).thenReturn(Optional.of(statusNew));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        orderCommandService.saveNewOrder(55L, principal, vehicle, services);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());

        Order capturedOrder = orderCaptor.getValue();
        assertEquals(55L, capturedOrder.getStationId());
        assertEquals(clientId, capturedOrder.getClientId());
        assertEquals(1, capturedOrder.getOrderItems().size());
        assertEquals("NEW", capturedOrder.getStatus().getId());

        verify(outboxEventService).saveOrderStatusEvent(any(OrderStatusChangeEvent.class));
    }

    @Test
    @DisplayName("saveNewOrder: Ошибка, если дефолтный статус 'NEW' отсутствует в системе")
    void saveNewOrder_StatusNotFound() {
        UserPrincipal principal = new UserPrincipal(clientId, "test@test.com", null, List.of());
        when(orderStatusRepository.findById("NEW")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                orderCommandService.saveNewOrder(1L, principal, new VehicleDto(1L, "A", "B", "C"), List.of())
        );

        verifyNoInteractions(orderRepository);
        verifyNoInteractions(outboxEventService);
    }

    @Test
    @DisplayName("updateOrderStatus: Успешная смена статуса существующего заказа")
    void updateOrderStatus_Success() {
        // Given
        Order dbOrder = new Order();
        dbOrder.setId(orderId);
        dbOrder.setClientId(clientId);

        OrderStatus targetStatus = new OrderStatus();
        targetStatus.setId("IN_PROGRESS");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(dbOrder));
        when(orderStatusRepository.findById("IN_PROGRESS")).thenReturn(Optional.of(targetStatus));

        orderCommandService.updateOrderStatus(orderId, "IN_PROGRESS", "client@test.com");

        assertEquals(targetStatus, dbOrder.getStatus());
        verify(orderRepository).save(dbOrder);
        verify(outboxEventService).saveOrderStatusEvent(argThat(event ->
                event.orderId().equals(orderId) &&
                        event.newStatus().getId().equals("IN_PROGRESS") &&
                        event.userEmail().equals("client@test.com")
        ));
    }

    @Test
    @DisplayName("updateOrderDetails: Изменение статуса + полная очистка списка мастеров")
    void updateOrderDetails_StatusChanged_And_ClearWorkers() {
        // Given
        Order order = new Order();
        order.setId(orderId);
        order.setClientId(clientId);
        Order spyOrder = spy(order);

        OrderStatus newStatus = new OrderStatus();
        newStatus.setId("COMPLETED");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(spyOrder));
        when(orderStatusRepository.findById("COMPLETED")).thenReturn(Optional.of(newStatus));

        orderCommandService.updateOrderDetails(
                orderId, "COMPLETED", true, "client@test.com", Set.of(), null
        );

        verify(spyOrder).setStatus(newStatus);
        verify(outboxEventService).saveOrderStatusEvent(any(OrderStatusChangeEvent.class));
        verify(spyOrder).clearWorkers();
        verify(outboxEventService, never()).saveWorkerAssignmentEvents(anyList());
        verify(orderRepository).save(spyOrder);
    }

    @Test
    @DisplayName("updateOrderDetails: Статус не меняется + успешное переназначение мастеров")
    void updateOrderDetails_StatusUnchanged_And_ReplaceWorkers() {
        Order order = new Order();
        order.setId(orderId);
        Order spyOrder = spy(order);

        Set<UUID> workerIds = Set.of(UUID.randomUUID(), UUID.randomUUID());
        Map<UUID, String> emailsMap = Map.of(UUID.randomUUID(), "w1@test.com");
        ValidationResponse validationResponse = new ValidationResponse(true, emailsMap);

        List<WorkerAssignmentEvent> expectedEvents = List.of(new WorkerAssignmentEvent(orderId, "w1@test.com"));

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(spyOrder));

        doReturn(expectedEvents).when(spyOrder).replaceWorkers(eq(workerIds), eq(emailsMap));

        orderCommandService.updateOrderDetails(
                orderId, "SAME_STATUS", false, null, workerIds, validationResponse
        );

        verify(orderStatusRepository, never()).findById(anyString());
        verify(outboxEventService, never()).saveOrderStatusEvent(any());

        verify(spyOrder).replaceWorkers(workerIds, validationResponse.emails());
        verify(outboxEventService).saveWorkerAssignmentEvents(expectedEvents);
        verify(orderRepository).save(spyOrder);
    }

    @Test
    @DisplayName("getOrderOrThrow: Исключение EntityNotFoundException, если заказ отсутствует")
    void getOrderOrThrow_ThrowsException() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                orderCommandService.updateOrderStatus(orderId, "ANY", "any@test.com")
        );

        verifyNoInteractions(orderStatusRepository);
        verifyNoInteractions(outboxEventService);
    }
}