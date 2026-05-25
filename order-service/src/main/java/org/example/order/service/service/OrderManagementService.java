package org.example.order.service.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.order.service.dto.request.PutOrderRequestDto;
import org.example.order.service.dto.request.RequestOrderDto;
import org.example.order.service.dto.request.RequestOrderStatusDto;
import org.example.order.service.dto.request.RequestVehicleDto;
import org.example.order.service.dto.response.OrderItemDto;
import org.example.order.service.dto.response.ResponseOrderDto;
import org.example.order.service.dto.response.ResponseOrderSummaryDto;
import org.example.order.service.entity.Order;
import org.example.order.service.entity.OrderItem;
import org.example.order.service.entity.OrderStatus;
import org.example.order.service.mapper.OrderItemMapper;
import org.example.order.service.mapper.OrderMapper;
import org.example.order.service.repository.OrderRepository;
import org.example.order.service.repository.OrderStatusRepository;
import org.example.securitycommon.UserPrincipal;
import org.example.station.service.api.common.client.StationServiceClient;
import org.example.station.service.api.common.dto.request.RequestOrderMappingStationDto;
import org.example.station.service.api.common.dto.response.SummaryResponseStationDto;
import org.example.station.service.api.common.dto.response.ServiceDetailDto;
import org.example.station.service.api.common.dto.response.StationServicesResponse;
import org.example.user.api.client.UserServiceFeignClient;
import org.example.user.api.requestDto.CarRequestDto;
import org.example.user.api.requestDto.OrderUserMappingRequest;
import org.example.user.api.requestDto.OrderVehicleMappingRequest;
import org.example.user.api.responceDto.OrderInfoFromUserServiceDto;
import org.example.user.api.responceDto.ValidationResponse;
import org.example.user.api.responceDto.VehicleDto;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderManagementService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserServiceFeignClient userServiceClient;
    private final OrderRepository orderRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final StationServiceClient stationServiceClient;
    private final StationIntegrationWrapper stationIntegrationWrapper;
    private final CacheManager cacheManager;

    @Transactional(readOnly = true)
    @Cacheable(value = "order-service:order",key = "#id")
    public ResponseOrderDto find(Long id) {
        Order dbOrder = getOrderOrThrow(id);
        OrderInfoFromUserServiceDto response = userServiceClient.getOrderInfo(buildUserRequest(dbOrder));

        List<OrderItemDto> orderItems = orderItemMapper.toDtoList(dbOrder.getOrderItems());

        return orderMapper.toResponseOrderDto(dbOrder,response,orderItems);
    }

    @Transactional
    public void createOrder(RequestOrderDto requestOrderDto, UserPrincipal userPrincipal) {
        Order order = new Order();

        RequestVehicleDto vehicle = requestOrderDto.vehicle();
        CarRequestDto vehicleRequest = new CarRequestDto(vehicle.make(), vehicle.model(), vehicle.number(), userPrincipal.userId());
        VehicleDto vehicleResponse = userServiceClient.getOrCreateCar(vehicleRequest);

        StationServicesResponse stationResponse = stationIntegrationWrapper.getValidatedServices(
                requestOrderDto.stationId(),
                requestOrderDto.serviceId()
        );

        if (!stationResponse.stationExists()) {
            throw new EntityNotFoundException("Станция не найдена");
        }
        List<OrderItem> orderItems = mapToOrderItem(stationResponse.services());
        OrderStatus status = getStatusOrThrow("NEW");

        order.setVehicleId(vehicleResponse.id());
        order.setOrderItems(orderItems);
        order.setClientId(userPrincipal.userId());
        order.setWorkerIds(new HashSet<>());
        order.setStatus(status);
        order.setStationId(requestOrderDto.stationId());

        orderRepository.save(order);
    }

    List<OrderItem> mapToOrderItem(List<ServiceDetailDto> services){
        return services.stream()
                .map(serviceDto -> {
                    OrderItem item = new OrderItem();
                    item.setServiceId(serviceDto.id());
                    item.setServiceName(serviceDto.name());
                    item.setPriceAtOrder(serviceDto.price());
                    return item;
                })
                .toList();
    }

    @Transactional
    @CacheEvict(value = "order-service:order",key = "#id")
    public void updateStatus(Long id, RequestOrderStatusDto status) {
        Order dbOrder = getOrderOrThrow(id);
        OrderStatus dbStatus = getStatusOrThrow(status.id());

        dbOrder.setStatus(dbStatus);
    }

    @Transactional
    @CacheEvict(value = "order-service:order",key = "#orderId")
    public void updateOrder(PutOrderRequestDto requestDto, Long orderId) {
        Order order = getOrderOrThrow(orderId);

        if(!order.getStatus().getId().equals(requestDto.statusId())){
            order.setStatus(getStatusOrThrow(requestDto.statusId()));
        }

        Set<UUID> workerIds = requestDto.workersId();

        if(workerIds.isEmpty()){
            order.clearWorkers();
            return;
        }

        ValidationResponse response = userServiceClient.validateWorkers(workerIds);

        if(response.exists()){
            order.replaceWorkers(workerIds,response.emails());
        }

        else {
            throw new EntityNotFoundException("Неверные id работников");
        }

    }

    private Order getOrderOrThrow(Long id){
        return orderRepository.findById(id).orElseThrow(()-> new EntityNotFoundException("Заказ с таким id не был найден"));
    }

    private OrderStatus getStatusOrThrow(String id){
        return orderStatusRepository.findById(id).orElseThrow(()-> new EntityNotFoundException("Статус с таким id не был найден"));
    }

    @Transactional(readOnly = true)
    public List<ResponseOrderDto> findWorkerOrder(UserPrincipal userPrincipal) {
        List<Order> orders = orderRepository.findAllByWorkerId(userPrincipal.userId());

        if (orders.isEmpty()) return new ArrayList<>();

        return getResponseOrderDto(orders);
    }

    @Transactional(readOnly = true)
    public List<ResponseOrderDto> findAll(Long stationId) {
        List<Order> orders = orderRepository.findAllByStationId(stationId);

        if (orders.isEmpty()) return new ArrayList<>();

        return getResponseOrderDto(orders);
    }

    private List<ResponseOrderDto> getResponseOrderDto(List<Order> orders){
        List<OrderUserMappingRequest> requests = orders.stream().map(this::buildUserRequest).toList();
        Map<Long,OrderInfoFromUserServiceDto> response = userServiceClient.getOrdersInfo(requests);

        return orders.stream()
                .map(order -> orderMapper.toResponseOrderDto
                        (order,
                                response.get(order.getId()),
                                orderItemMapper.toDtoList(order.getOrderItems()))
                ).toList();
    }

    private OrderUserMappingRequest buildUserRequest(Order order){
        return new OrderUserMappingRequest(order.getId(),order.getClientId(),order.getWorkerIds(),order.getVehicleId());
    }

    @Transactional(readOnly = true)
    public List<ResponseOrderSummaryDto> findUserOrder(UserPrincipal userPrincipal) {
        List<Order> orders = orderRepository.findAllByClientId(userPrincipal.userId());

        if (orders.isEmpty()) return new ArrayList<>();

        List<OrderVehicleMappingRequest> userRequest = orders.stream().map(this::buildVehicleRequest).toList();
        Map<Long, VehicleDto> userResponse = userServiceClient.getCarsInfo(userRequest);

        List<RequestOrderMappingStationDto> stationRequest = orders.stream().map(this::buildStationRequest).toList();
        Map<Long, SummaryResponseStationDto> stationResponse = stationServiceClient.getStationsByOrders(stationRequest);

        return orders.stream()
                .map(order -> {
                    VehicleDto vehicle = userResponse.get(order.getVehicleId());
                    SummaryResponseStationDto station = stationResponse.get(order.getStationId());
                    List<OrderItemDto> services = orderItemMapper.toDtoList(order.getOrderItems());

                    return orderMapper.toResponseOrderSummaryDto(order, vehicle, station, services);
                })
                .toList();
    }


    private OrderVehicleMappingRequest buildVehicleRequest(Order order){
        return new OrderVehicleMappingRequest(order.getId(), order.getVehicleId());
    }

    private RequestOrderMappingStationDto buildStationRequest(Order order){
        return new RequestOrderMappingStationDto(order.getId(), order.getStationId());
    }

    @Transactional
    public void deleteByStation(Long id) {
        List<Order> orders = orderRepository.deleteAllByStationId(id);
        clearOrderCache(orders);
    }

    @Transactional
    public void deleteOrderByClient(UUID userId) {
        List<Order> orders = orderRepository.deleteAllByClientId(userId);
        clearOrderCache(orders);
    }

    private void clearOrderCache(List<Order> orders){
        if (orders == null || orders.isEmpty()) {
            return;
        }
        Cache cache = cacheManager.getCache("order-service:order");

        if(cache!=null) {
            orders.forEach(o-> cache.evict(o.getId()));
        }

    }

}
