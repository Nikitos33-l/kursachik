package org.example.orderservice.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.orderservice.client.UserServiceClient;
import org.example.orderservice.dto.external.UserDto;
import org.example.orderservice.dto.external.VehicleDto;
import org.example.orderservice.dto.request.RequestOrderStatusDto;
import org.example.orderservice.dto.response.OrderItemDto;
import org.example.orderservice.dto.response.ResponseOrderDto;
import org.example.orderservice.entity.Order;
import org.example.orderservice.entity.OrderStatus;
import org.example.orderservice.mapper.OrderItemMapper;
import org.example.orderservice.mapper.OrderMapper;
import org.example.orderservice.repository.OrderRepository;
import org.example.orderservice.repository.OrderStatusRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderManagementService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserServiceClient userServiceClient;
    private final OrderRepository orderRepository;
    private final OrderStatusRepository orderStatusRepository;

    @Transactional
    public ResponseOrderDto find(Long id) {
        Order dbOrder = getOrderOrThrow(id);
        List<UserDto> workers = userServiceClient.findByIds(dbOrder.getWorkerIds());
        UserDto client = userServiceClient.findById(dbOrder.getClientId());
        VehicleDto vehicle = userServiceClient.findCar(dbOrder.getVehicleId());

        List<OrderItemDto> orderItems = dbOrder.getOrderItems().
                stream().map(orderItemMapper::toDto).toList();

        return orderMapper.toResponseOrderDto(dbOrder,vehicle,client,workers,orderItems);
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
}
