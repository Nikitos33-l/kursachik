package org.example.order.service.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.order.service.dto.request.RequestOrderStatusDto;
import org.example.order.service.dto.response.ResponseOrderDto;
import org.example.order.service.entity.Order;
import org.example.order.service.mapper.OrderItemMapper;
import org.example.order.service.mapper.OrderMapper;
import org.example.order.service.repository.OrderRepository;
import org.example.order.service.repository.OrderStatusRepository;
import org.example.user.api.client.UserServiceFeignClient;
import org.example.user.api.responceDto.OrderInfoFromUserServiceDto;
import org.example.user.api.responceDto.UserDto;
import org.example.user.api.responceDto.VehicleDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @InjectMocks
    private OrderManagementService orderManagementService;

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

    private Order createOrder(Long id) {
        Order order = new Order();
        order.setId(id);
        order.setClientId(100L);
        order.setWorkerIds(Set.of(200L));
        order.setVehicleId(300L);
        order.setOrderItems(new ArrayList<>());
        return order;
    }

    private OrderInfoFromUserServiceDto createUserInfo() {
        return new OrderInfoFromUserServiceDto(
                new UserDto(100L, "client@test.com", "Client Name"),
                List.of(new UserDto(200L, "worker@test.com", "Worker Name")),
                new VehicleDto(300L, "BMW", "X5", "7777-7")
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