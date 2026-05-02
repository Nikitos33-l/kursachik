package org.example.order.service.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.order.service.dto.request.RequestOrderStatusDto;
import org.example.order.service.dto.response.OrderItemDto;
import org.example.order.service.dto.response.ResponseOrderDto;
import org.example.order.service.dto.response.ResponseOrderSummaryDto;
import org.example.order.service.entity.Order;
import org.example.order.service.entity.OrderStatus;
import org.example.order.service.mapper.OrderItemMapper;
import org.example.order.service.mapper.OrderMapper;
import org.example.order.service.repository.OrderRepository;
import org.example.order.service.repository.OrderStatusRepository;
import org.example.securitycommon.UserPrincipal;
import org.example.user.api.client.UserServiceFeignClient;
import org.example.user.api.requestDto.OrderUserMappingRequest;
import org.example.user.api.requestDto.OrderVehicleMappingRequest;
import org.example.user.api.responceDto.OrderInfoFromUserServiceDto;
import org.example.user.api.responceDto.VehicleDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderManagementService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserServiceFeignClient userServiceClient;
    private final OrderRepository orderRepository;
    private final OrderStatusRepository orderStatusRepository;

    @Transactional
    public ResponseOrderDto find(Long id) {
        Order dbOrder = getOrderOrThrow(id);
        OrderInfoFromUserServiceDto response = userServiceClient.getOrderInfo(buildRequest(dbOrder));

        List<OrderItemDto> orderItems = dbOrder.getOrderItems().
                stream().map(orderItemMapper::toDto).toList();

        return orderMapper.toResponseOrderDto(dbOrder,response,orderItems);
    }

    @Transactional
    public void updateStatus(Long id, RequestOrderStatusDto status) {
        Order dbOrder = getOrderOrThrow(id);
        OrderStatus dbStatus = getStatusOrThrow(status.id());

        dbOrder.setStatus(dbStatus);
    }

    private Order getOrderOrThrow(Long id){
        return orderRepository.findById(id).orElseThrow(()-> new EntityNotFoundException("Заказ с таким id не был найден"));
    }

    private OrderStatus getStatusOrThrow(String id){
        return orderStatusRepository.findById(id).orElseThrow(()-> new EntityNotFoundException("Статус с таким id не был найден"));
    }

    @Transactional
    public List<ResponseOrderDto> findAll(Long stationId) {
        List<Order> orders = orderRepository.findAllByStationId(stationId);
        List<OrderUserMappingRequest> requests = orders.stream().map(this::buildRequest).toList();
        Map<Long,OrderInfoFromUserServiceDto> response = userServiceClient.getOrdersInfo(requests);
        return orders.stream()
                .map(order -> orderMapper.toResponseOrderDto
                        (order,
                                response.get(order.getId()),
                                order.getOrderItems().stream().map(orderItemMapper::toDto).toList())
                ).toList();
    }

    private OrderUserMappingRequest buildRequest(Order order){
        return new OrderUserMappingRequest(order.getId(),order.getClientId(),order.getWorkerIds(),order.getVehicleId());
    }

    @Transactional
    public List<ResponseOrderSummaryDto> findUserOrder(UserPrincipal userPrincipal) {
        List<Order> orders = orderRepository.findAllByClientId(userPrincipal.userId());
        List<OrderVehicleMappingRequest> request = orders.stream().map(this::buildVehicleRequest).toList();
        Map<Long, VehicleDto> response = userServiceClient.getCarsInfo(request);

    }

    private OrderVehicleMappingRequest buildVehicleRequest(Order order){
        return new OrderVehicleMappingRequest(order.getId(), order.getVehicleId());
    }
}
