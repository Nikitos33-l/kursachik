package org.example.order.service.service;

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
import org.example.order.service.event.OrderStatusChangeEvent;
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
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final CacheManager cacheManager;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    @Cacheable(value = ORDER_CACHE, key = "#id")
    public ResponseOrderDto find(Long id) {
        log.info("[CACHE MISS] Запрос информации о заказе ID: {} напрямую из базы и внешних сервисов", id);
        Order dbOrder = getOrderOrThrow(id);

        OrderInfoFromUserServiceDto response = userServiceClient.getOrderInfo(buildUserRequest(dbOrder));
        List<OrderItemDto> orderItems = orderItemMapper.toDtoList(dbOrder.getOrderItems());

        return orderMapper.toResponseOrderDto(dbOrder, response, orderItems);
    }

    @Transactional
    public void createOrder(RequestOrderDto requestOrderDto, UserPrincipal userPrincipal) {
        log.info("Старт создания нового заказа для клиента UUID: {} на СТО ID: {}", userPrincipal.userId(), requestOrderDto.stationId());
        Order order = new Order();

        RequestVehicleDto vehicle = requestOrderDto.vehicle();
        CarRequestDto vehicleRequest = new CarRequestDto(vehicle.make(), vehicle.model(), vehicle.number(), userPrincipal.userId());

        log.debug("Запрос/создание автомобиля через User Service: {} {} [{}]", vehicle.make(), vehicle.model(), vehicle.number());
        VehicleDto vehicleResponse = userServiceClient.getOrCreateCar(vehicleRequest);

        log.debug("Валидация списка выбранных услуг {} на СТО ID: {}", requestOrderDto.serviceId(), requestOrderDto.stationId());
        StationServicesResponse stationResponse = stationIntegrationWrapper.getValidatedServices(
                requestOrderDto.stationId(),
                requestOrderDto.serviceId()
        );

        if (!stationResponse.stationExists()) {
            log.error("Ошибка создания заказа: Станция с ID {} не найдена", requestOrderDto.stationId());
            throw new EntityNotFoundException("Станция не найдена");
        }

        List<OrderItem> orderItems = mapToOrderItem(stationResponse.services());
        OrderStatus status = getStatusOrThrow("NEW");

        order.setVehicleId(vehicleResponse.id());
        order.setOrderItems(orderItems);
        order.setClientId(userPrincipal.userId());
        order.setWorkerIds(new HashSet<>());
        order.setStatus(status, userPrincipal.email());
        order.setStationId(requestOrderDto.stationId());

        Order savedOrder = orderRepository.save(order);
        log.info("Заказ успешно сохранен в БД. Присвоен ID: {}, Статус: NEW", savedOrder.getId());

        log.debug("Публикация внутреннего Spring Event изменения статуса для заказа ID: {}", savedOrder.getId());
        eventPublisher.publishEvent(new OrderStatusChangeEvent(
                savedOrder.getId(),
                savedOrder.getStatus(),
                savedOrder.getClientId(),
                userPrincipal.email()
        ));
    }

    List<OrderItem> mapToOrderItem(List<ServiceDetailDto> services) {
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
    @CacheEvict(value = ORDER_CACHE, key = "#id")
    public void updateStatus(Long id, RequestOrderStatusDto status) {
        log.info("Запрос на обновление статуса заказа ID: {}. Целевой статус: {}", id, status.id());
        Order dbOrder = getOrderOrThrow(id);
        OrderStatus dbStatus = getStatusOrThrow(status.id());

        String oldStatus = dbOrder.getStatus() != null ? dbOrder.getStatus().getId() : "UNKNOWN";
        dbOrder.setStatus(dbStatus);

        orderRepository.save(dbOrder);
        log.info("Статус заказа ID: {} успешно изменен с '{}' на '{}'. Кэш инвалидирован.", id, oldStatus, status.id());
    }

    @Transactional
    @CacheEvict(value = ORDER_CACHE, key = "#orderId")
    public void updateOrder(PutOrderRequestDto requestDto, Long orderId) {
        log.info("Административное обновление заказа ID: {}. Новый статус: {}", orderId, requestDto.statusId());
        Order order = getOrderOrThrow(orderId);

        if (!order.getStatus().getId().equals(requestDto.statusId())) {
            log.debug("Смена статуса в рамках редактирования заказа: '{}' -> '{}'", order.getStatus().getId(), requestDto.statusId());
            order.setStatus(getStatusOrThrow(requestDto.statusId()));
        }

        Set<UUID> workerIds = requestDto.workersId();
        if (workerIds.isEmpty()) {
            log.info("Список мастеров пуст. Удаление всех назначенных механиков с заказа ID: {}", orderId);
            order.clearWorkers();
            orderRepository.save(order);
            return;
        }

        log.debug("Валидация списка мастеров {} через User Service", workerIds);
        ValidationResponse response = userServiceClient.validateWorkers(workerIds);
        if (response.exists()) {
            order.replaceWorkers(workerIds, response.emails());
            log.info("На заказ ID: {} успешно назначены мастера: {}", orderId, response.emails());
        } else {
            log.warn("Провал обновления заказа ID: {}. Передан некорректный список UUID работников: {}", orderId, workerIds);
            throw new EntityNotFoundException("Неверные id работников");
        }

        orderRepository.save(order);
    }

    private Order getOrderOrThrow(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> {
            log.warn("Заказ с ID: {} не найден в базе данных", id);
            return new EntityNotFoundException("Заказ с таким id не был найден");
        });
    }

    private OrderStatus getStatusOrThrow(String id) {
        return orderStatusRepository.findById(id).orElseThrow(() -> {
            log.warn("Статус заказа со строковым кодом '{}' не зарегистрирован в системе", id);
            return new EntityNotFoundException("Статус с таким id не был найден");
        });
    }

    @Transactional(readOnly = true)
    public List<ResponseOrderDto> findWorkerOrder(UserPrincipal userPrincipal) {
        log.debug("Загрузка заказов из БД для автомеханика UUID: {}", userPrincipal.userId());
        List<Order> orders = orderRepository.findAllByWorkerId(userPrincipal.userId());

        if (orders.isEmpty()) {
            return new ArrayList<>();
        }

        return getResponseOrderDto(orders);
    }

    @Transactional(readOnly = true)
    public List<ResponseOrderDto> findAll(Long stationId) {
        log.debug("Загрузка всех заказов для СТО с ID: {}", stationId);
        List<Order> orders = orderRepository.findAllByStationId(stationId);

        if (orders.isEmpty()) {
            return new ArrayList<>();
        }

        return getResponseOrderDto(orders);
    }

    private List<ResponseOrderDto> getResponseOrderDto(List<Order> orders) {
        log.debug("Запрос детальной информации о пользователях/машинах для пачки из {} заказов через Feign", orders.size());
        List<OrderUserMappingRequest> requests = orders.stream().map(this::buildUserRequest).toList();
        Map<Long, OrderInfoFromUserServiceDto> response = userServiceClient.getOrdersInfo(requests);

        return orders.stream()
                .map(order -> orderMapper.toResponseOrderDto(
                        order,
                        response.get(order.getId()),
                        orderItemMapper.toDtoList(order.getOrderItems()))
                ).toList();
    }

    private OrderUserMappingRequest buildUserRequest(Order order) {
        return new OrderUserMappingRequest(order.getId(), order.getClientId(), order.getWorkerIds(), order.getVehicleId());
    }

    @Transactional(readOnly = true)
    public List<ResponseOrderSummaryDto> findUserOrder(UserPrincipal userPrincipal) {
        log.debug("Сбор агрегированной истории заказов для клиента UUID: {}", userPrincipal.userId());
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

    private OrderVehicleMappingRequest buildVehicleRequest(Order order) {
        return new OrderVehicleMappingRequest(order.getId(), order.getVehicleId());
    }

    private RequestOrderMappingStationDto buildStationRequest(Order order) {
        return new RequestOrderMappingStationDto(order.getId(), order.getStationId());
    }

    @Transactional
    public void deleteByStation(Long id) {
        log.info("[CASCADE DELETE] Инициация каскадного удаления заказов для СТО ID: {}", id);
        List<Order> orders = orderRepository.deleteAllByStationId(id);
        log.info("[CASCADE DELETE] Из БД удалено {} заказов, привязанных к СТО ID: {}", orders.size(), id);
        clearOrderCache(orders);
    }

    @Transactional
    public void deleteOrderByClient(UUID userId) {
        log.info("[CASCADE DELETE] Инициация каскадного удаления заказов для клиента UUID: {}", userId);
        List<Order> orders = orderRepository.deleteAllByClientId(userId);
        log.info("[CASCADE DELETE] Из БД удалено {} заказов клиента UUID: {}", orders.size(), userId);
        clearOrderCache(orders);
    }

    private void clearOrderCache(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        Cache cache = cacheManager.getCache(ORDER_CACHE);
        if (cache != null) {
            log.info("Очистка {} записей из кэша Спринга [{}]", orders.size(), ORDER_CACHE);
            orders.forEach(o -> cache.evict(o.getId()));
        }
    }
}