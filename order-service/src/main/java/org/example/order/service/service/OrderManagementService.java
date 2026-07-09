package org.example.order.service.service;

import com.example.order.service.api.common.dto.OrderTotalResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.math.BigDecimal;
import java.util.*;

import static org.example.order.service.constant.CacheNames.*;

@Slf4j
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
    private final UserIntegrationWrapper userIntegrationWrapper;
    private final CacheManager cacheManager;

    private final OrderCommandService orderCommandService;

    public void createOrder(RequestOrderDto requestOrderDto, UserPrincipal userPrincipal) {
        log.info("Старт создания нового заказа для клиента UUID: {} на СТО ID: {}", userPrincipal.userId(), requestOrderDto.stationId());

        RequestVehicleDto vehicle = requestOrderDto.vehicle();
        CarRequestDto vehicleRequest = new CarRequestDto(vehicle.make(), vehicle.model(), vehicle.number(), userPrincipal.userId());
        VehicleDto vehicleResponse = userServiceClient.getOrCreateCar(vehicleRequest);

        StationServicesResponse stationResponse = stationIntegrationWrapper.getValidatedServices(
                requestOrderDto.stationId(), requestOrderDto.serviceId()
        );

        if (!stationResponse.stationExists()) {
            throw new EntityNotFoundException("Станция не найдена");
        }

        orderCommandService.saveNewOrder(requestOrderDto.stationId(), userPrincipal, vehicleResponse, stationResponse.services());
    }

    @CacheEvict(value = ORDER_CACHE, key = "#id")
    public void updateStatus(Long id, RequestOrderStatusDto status) {
        log.info("Запрос на обновление статуса заказа ID: {}. Целевой статус: {}", id, status.id());

        Order initialOrder = getOrderOrThrow(id);
        if (initialOrder.getStatus() != null && initialOrder.getStatus().getId().equals(status.id())) {
            log.info("Статус заказа ID: {} уже равен {}. Отмена операции.", id, status.id());
            return;
        }

        String clientEmail = userIntegrationWrapper.getEmailByUserId(initialOrder.getClientId());

        orderCommandService.updateOrderStatus(id, status.id(), clientEmail);
    }

    @CacheEvict(value = ORDER_CACHE, key = "#orderId")
    public void updateOrder(PutOrderRequestDto requestDto, Long orderId) {
        log.info("Административное обновление заказа ID: {}", orderId);

        Order initialOrder = getOrderOrThrow(orderId);
        boolean isStatusChanged = !initialOrder.getStatus().getId().equals(requestDto.statusId());

        String clientEmail = isStatusChanged ? userIntegrationWrapper.getEmailByUserId(initialOrder.getClientId()) : null;

        Set<UUID> workerIds = requestDto.workersId();
        ValidationResponse validationResponse = null;

        if (!workerIds.isEmpty()) {
            validationResponse = userServiceClient.validateWorkers(workerIds);
            if (!validationResponse.exists()) {
                throw new EntityNotFoundException("Неверные id работников");
            }
        }

        orderCommandService.updateOrderDetails(orderId, requestDto.statusId(), isStatusChanged, clientEmail, workerIds, validationResponse);
    }


    @Transactional(readOnly = true)
    @Cacheable(value = ORDER_CACHE, key = "#id")
    public ResponseOrderDto find(Long id) {
        log.info("[CACHE MISS] Запрос информации о заказе ID: {}", id);
        Order dbOrder = getOrderOrThrow(id);
        OrderInfoFromUserServiceDto response = userServiceClient.getOrderInfo(buildUserRequest(dbOrder));
        List<OrderItemDto> orderItems = orderItemMapper.toDtoList(dbOrder.getOrderItems());
        return orderMapper.toResponseOrderDto(dbOrder, response, orderItems);
    }

    @Transactional
    public void handleOrderPaid(Long id){
        Order order = getOrderOrThrow(id);
        OrderStatus closeOrderStatus = orderStatusRepository.
                findById("CLOSED").orElseThrow(()->new EntityNotFoundException("Статус с таким id не найден"));
        order.setStatus(closeOrderStatus);
        log.info("Статус заказа успешно сменен на закрыт.ID {}",id);
    }


    @Transactional(readOnly = true)
    public OrderTotalResponse getTotal(Long orderId) {
        log.info("[БД] Подсчет общей стоимости для заказа ID: {}", orderId);

        Order order = getOrderOrThrow(orderId);

        BigDecimal total = order.getOrderItems().stream()
                .map(OrderItem::getPriceAtOrder)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.debug("[БД] Итоговая стоимость заказа ID: {} составила {} BYN", orderId, total);

        return new OrderTotalResponse(total);
    }

    private Order getOrderOrThrow(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Заказ с таким id не был найден"));
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

    private List<ResponseOrderDto> getResponseOrderDto(List<Order> orders) {
        List<OrderUserMappingRequest> requests = orders.stream().map(this::buildUserRequest).toList();
        Map<Long, OrderInfoFromUserServiceDto> response = userServiceClient.getOrdersInfo(requests);
        return orders.stream()
                .map(order -> orderMapper.toResponseOrderDto(order, response.get(order.getId()), orderItemMapper.toDtoList(order.getOrderItems())))
                .toList();
    }

    private OrderUserMappingRequest buildUserRequest(Order order) {
        return new OrderUserMappingRequest(order.getId(), order.getClientId(), order.getWorkerIds(), order.getVehicleId());
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
                .map(order -> orderMapper.toResponseOrderSummaryDto(order, userResponse.get(order.getVehicleId()), stationResponse.get(order.getStationId()), orderItemMapper.toDtoList(order.getOrderItems())))
                .toList();
    }

    private OrderVehicleMappingRequest buildVehicleRequest(Order order) {
        return new OrderVehicleMappingRequest(order.getId(), order.getVehicleId());
    }

    private RequestOrderMappingStationDto buildStationRequest(Order order) {
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

    private void clearOrderCache(List<Order> orders) {
        if (orders == null || orders.isEmpty()) return;
        Cache cache = cacheManager.getCache(ORDER_CACHE);
        if (cache != null) {
            orders.forEach(o -> cache.evict(o.getId()));
        }
    }




}