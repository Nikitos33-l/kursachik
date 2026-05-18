package org.example.order.service.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.order.service.dto.request.PutOrderRequestDto;
import org.example.order.service.dto.request.RequestOrderDto;
import org.example.order.service.dto.request.RequestOrderStatusDto;
import org.example.order.service.dto.request.RequestVehicleDto;
import org.example.order.service.dto.response.ResponseOrderDto;
import org.example.order.service.dto.response.ResponseOrderSummaryDto;
import org.example.order.service.entity.Order;
import org.example.order.service.entity.OrderStatus;
import org.example.order.service.mapper.OrderItemMapper;
import org.example.order.service.mapper.OrderMapper;
import org.example.order.service.repository.OrderRepository;
import org.example.order.service.repository.OrderStatusRepository;
import org.example.securitycommon.UserPrincipal;
import org.example.station.service.api.common.client.StationServiceClient;
import org.example.station.service.api.common.dto.response.SummaryResponseStationDto;
import org.example.station.service.api.common.dto.response.ServiceDetailDto;
import org.example.station.service.api.common.dto.response.StationServicesResponse;
import org.example.order.service.service.OrderManagementService;
import org.example.user.api.client.UserServiceFeignClient;
import org.example.user.api.requestDto.CarRequestDto;
import org.example.user.api.responceDto.OrderInfoFromUserServiceDto;
import org.example.user.api.responceDto.UserDto;
import org.example.user.api.responceDto.ValidationResponse;
import org.example.user.api.responceDto.VehicleDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderManagementServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderStatusRepository orderStatusRepository;
    @Mock private UserServiceFeignClient userServiceClient;
    @Mock private OrderMapper orderMapper;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private StationServiceClient stationServiceClient;

    @InjectMocks
    private OrderManagementService orderManagementService;

    private final UUID clientId = UUID.randomUUID();
    private final UUID workerId = UUID.randomUUID();
    private final Long vehicleId = 300L;

    @Test
    @DisplayName("find: Успешный возврат обогащенного заказа")
    void find_Success() {
        Long id = 1L;
        Order order = createOrder(id);
        OrderInfoFromUserServiceDto info = createUserInfo();
        ResponseOrderDto expected = createResponseDto(id, "NEW", info);

        when(orderRepository.findById(id)).thenReturn(Optional.of(order));
        when(userServiceClient.getOrderInfo(any())).thenReturn(info);
        when(orderMapper.toResponseOrderDto(eq(order), eq(info), anyList())).thenReturn(expected);

        ResponseOrderDto actual = orderManagementService.find(id);

        assertEquals(expected, actual);
        assertEquals(id, actual.id());
    }

    @Test
    @DisplayName("findAll: Корректный маппинг списка заказов через Map")
    void findAll_Success() {
        Long orderId = 101L;
        Order order = createOrder(orderId);
        OrderInfoFromUserServiceDto info = createUserInfo();
        Map<Long, OrderInfoFromUserServiceDto> infoMap = Map.of(orderId, info);
        ResponseOrderDto expectedDto = createResponseDto(orderId, "IN_PROGRESS", info);

        when(orderRepository.findAllByStationId(anyLong())).thenReturn(List.of(order));
        when(userServiceClient.getOrdersInfo(anyList())).thenReturn(infoMap);
        when(orderMapper.toResponseOrderDto(eq(order), eq(info), anyList())).thenReturn(expectedDto);

        List<ResponseOrderDto> result = orderManagementService.findAll(10L);

        assertEquals(1, result.size());
        assertEquals(orderId, result.get(0).id());
        verify(orderMapper).toResponseOrderDto(eq(order), eq(info), anyList());
    }

    @Test
    @DisplayName("updateStatus: Ошибка, если статус не найден")
    void updateStatus_StatusNotFound() {
        when(orderRepository.findById(anyLong())).thenReturn(Optional.of(createOrder(1L)));
        when(orderStatusRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> orderManagementService.updateStatus(1L, new RequestOrderStatusDto("INVALID")));
    }

    @Test
    @DisplayName("createOrder: Успешное создание заказа")
    void createOrder_Success() {
        RequestVehicleDto vehicleDto = new RequestVehicleDto("BMW", "X5", "7777-7");
        RequestOrderDto requestDto = new RequestOrderDto(vehicleDto, List.of(1L, 2L), 10L);

        UserPrincipal principal = new UserPrincipal(clientId, "test@test.com", 1L, List.of());

        VehicleDto savedVehicle = new VehicleDto(vehicleId, "BMW", "X5", "7777-7");
        StationServicesResponse stationResponse = new StationServicesResponse(true, List.of(
                new ServiceDetailDto(1L, "Замена масла", new BigDecimal(50)),
                new ServiceDetailDto(2L, "Диагностика", new BigDecimal(30))
        ));
        OrderStatus newStatus = new OrderStatus();

        when(userServiceClient.getOrCreateCar(any(CarRequestDto.class))).thenReturn(savedVehicle);
        when(stationServiceClient.validateStationAndGetServices(eq(10L), anyList())).thenReturn(stationResponse);
        when(orderStatusRepository.findById("NEW")).thenReturn(Optional.of(newStatus));

        orderManagementService.createOrder(requestDto, principal);

        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("createOrder: Ошибка, если станция не найдена или не оказывает услуги")
    void createOrder_StationNotFound() {
        RequestOrderDto requestDto = new RequestOrderDto(new RequestVehicleDto("A", "B", "C"), List.of(1L), 99L);

        UserPrincipal principal = new UserPrincipal(clientId, "test@test.com", 1L, List.of());

        when(userServiceClient.getOrCreateCar(any())).thenReturn(new VehicleDto(vehicleId, "A", "B", "C"));
        when(stationServiceClient.validateStationAndGetServices(anyLong(), anyList()))
                .thenReturn(new StationServicesResponse(false, List.of()));

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> orderManagementService.createOrder(requestDto, principal));
        assertEquals("Станция не найдена", exception.getMessage());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateStatus: Успешное обновление статуса")
    void updateStatus_Success() {
        Long orderId = 1L;
        Order order = createOrder(orderId);
        OrderStatus newStatus = new OrderStatus();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderStatusRepository.findById("IN_PROGRESS")).thenReturn(Optional.of(newStatus));

        orderManagementService.updateStatus(orderId, new RequestOrderStatusDto("IN_PROGRESS"));

        assertEquals(newStatus, order.getStatus());
    }

    @Test
    @DisplayName("updateOrder: Успешное обновление с очисткой работников")
    void updateOrder_ClearWorkers_Success() {
        Long orderId = 1L;
        Order order = createOrder(orderId);
        OrderStatus currentStatus = createStatus();
        Order spyOrder = spy(order);
        doReturn(currentStatus).when(spyOrder).getStatus();

        PutOrderRequestDto requestDto = new PutOrderRequestDto(Collections.emptySet(),"COMPLETED");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(spyOrder));
        when(orderStatusRepository.findById("COMPLETED")).thenReturn(Optional.of(currentStatus));

        orderManagementService.updateOrder(requestDto, orderId);

        verify(spyOrder).clearWorkers();
        verify(userServiceClient, never()).validateWorkers(any());
    }

    @Test
    @DisplayName("updateOrder: Успешная замена работников")
    void updateOrder_ReplaceWorkers_Success() {
        Long orderId = 1L;
        Order order = createOrder(orderId);
        order.setWorkerIds(new HashSet<>(Set.of(workerId)));

        OrderStatus status = createStatus();
        Order spyOrder = spy(order);
        doReturn(status).when(spyOrder).getStatus();

        UUID newWorker1 = UUID.randomUUID();
        UUID newWorker2 = UUID.randomUUID();
        Set<UUID> newWorkers = Set.of(newWorker1, newWorker2);
        PutOrderRequestDto requestDto = new PutOrderRequestDto(newWorkers,"COMPLETED");

        ValidationResponse validationResponse = new ValidationResponse(true, Map.of(
                newWorker1, "worker1@test.com",
                newWorker2, "worker2@test.com"
        ));

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(spyOrder));
        when(orderStatusRepository.findById("COMPLETED")).thenReturn(Optional.of(status));
        when(userServiceClient.validateWorkers(newWorkers)).thenReturn(validationResponse);

        orderManagementService.updateOrder(requestDto, orderId);

        verify(spyOrder).replaceWorkers(newWorkers, validationResponse.emails());
    }

    @Test
    @DisplayName("updateOrder: Ошибка, если переданные работники не существуют")
    void updateOrder_WorkersNotFound() {
        Long orderId = 1L;
        Order order = createOrder(orderId);
        OrderStatus status = createStatus();
        Order spyOrder = spy(order);
        doReturn(status).when(spyOrder).getStatus();

        Set<UUID> invalidWorkers = Set.of(UUID.randomUUID());
        PutOrderRequestDto requestDto = new PutOrderRequestDto(invalidWorkers,"COMPLETED");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(spyOrder));
        when(orderStatusRepository.findById("COMPLETED")).thenReturn(Optional.of(status));
        when(userServiceClient.validateWorkers(invalidWorkers)).thenReturn(new ValidationResponse(false, Map.of()));

        assertThrows(EntityNotFoundException.class,
                () -> orderManagementService.updateOrder(requestDto, orderId));
    }

    @Test
    @DisplayName("findWorkerOrder: Возврат пустого списка, если заказов нет")
    void findWorkerOrder_Empty() {
        UserPrincipal principal = new UserPrincipal(workerId, "worker@test.com", 1L, List.of());
        when(orderRepository.findAllByWorkerId(principal.userId())).thenReturn(Collections.emptyList());

        List<ResponseOrderDto> result = orderManagementService.findWorkerOrder(principal);

        assertTrue(result.isEmpty());
        verify(userServiceClient, never()).getOrdersInfo(any());
    }

    @Test
    @DisplayName("findUserOrder: Успешный возврат сводки заказов клиента")
    void findUserOrder_Success() {
        Long orderId = 1L;
        Long stationId = 10L;

        UserPrincipal principal = new UserPrincipal(clientId, "client@test.com", null, List.of());

        Order order = createOrder(orderId);
        order.setVehicleId(vehicleId);
        order.setStationId(stationId);

        VehicleDto vehicleDto = new VehicleDto(vehicleId, "Audi", "A4", "1111-1");
        SummaryResponseStationDto stationDto = new SummaryResponseStationDto(stationId, "СТО на Немиге", "Минск");

        when(orderRepository.findAllByClientId(clientId)).thenReturn(List.of(order));

        when(userServiceClient.getCarsInfo(anyList())).thenReturn(Map.of(vehicleId, vehicleDto));
        when(stationServiceClient.getStationsByOrders(anyList())).thenReturn(Map.of(stationId, stationDto));

        ResponseOrderSummaryDto expectedSummary = new ResponseOrderSummaryDto(
                vehicleDto,
                List.of(),
                "NEW",
                stationDto.name()
        );

        when(orderMapper.toResponseOrderSummaryDto(eq(order), eq(vehicleDto), eq(stationDto), anyList()))
                .thenReturn(expectedSummary);

        List<ResponseOrderSummaryDto> result = orderManagementService.findUserOrder(principal);

        assertEquals(1, result.size());
        assertEquals(expectedSummary, result.get(0));
    }

    private OrderStatus createStatus(){
        OrderStatus status = new OrderStatus();
        status.setId("NEW");
        return status;
    }

    private Order createOrder(Long id) {
        Order order = new Order();
        order.setId(id);
        order.setClientId(clientId);
        order.setWorkerIds(new HashSet<>(Set.of(workerId)));
        order.setVehicleId(vehicleId); // Снова Long
        order.setOrderItems(new ArrayList<>());
        return order;
    }

    private OrderInfoFromUserServiceDto createUserInfo() {
        return new OrderInfoFromUserServiceDto(
                new UserDto(clientId, "client@test.com", "Client Name"),
                List.of(new UserDto(workerId, "worker@test.com", "Worker Name")),
                new VehicleDto(vehicleId, "BMW", "X5", "7777-7") // Снова Long
        );
    }

    private ResponseOrderDto createResponseDto(Long id, String status, OrderInfoFromUserServiceDto info) {
        return new ResponseOrderDto(
                id,
                status,
                info.getVehicle(),
                info.getClient(),
                info.getWorkers(),
                List.of()
        );
    }
}