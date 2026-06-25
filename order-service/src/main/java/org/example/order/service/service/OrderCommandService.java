package org.example.order.service.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.service.entity.Order;
import org.example.order.service.entity.OrderItem;
import org.example.order.service.entity.OrderStatus;
import org.example.order.service.event.OrderStatusChangeEvent;
import org.example.order.service.event.WorkerAssignmentEvent;
import org.example.order.service.repository.OrderRepository;
import org.example.order.service.repository.OrderStatusRepository;
import org.example.securitycommon.UserPrincipal;
import org.example.station.service.api.common.dto.response.ServiceDetailDto;
import org.example.user.api.responceDto.ValidationResponse;
import org.example.user.api.responceDto.VehicleDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderCommandService {

    private final OrderRepository orderRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final OutboxEventService outboxEventService;

    public void saveNewOrder(Long stationId, UserPrincipal principal, VehicleDto vehicle, List<ServiceDetailDto> services) {
        Order order = new Order();
        order.setVehicleId(vehicle.id());
        order.setOrderItems(mapToOrderItem(services));
        order.setClientId(principal.userId());
        order.setWorkerIds(new HashSet<>());
        order.setStatus(getStatusOrThrow("NEW"));
        order.setStationId(stationId);

        Order savedOrder = orderRepository.save(order);
        log.info("Заказ успешно сохранен в БД через CommandService. ID: {}", savedOrder.getId());

        outboxEventService.saveOrderStatusEvent(new OrderStatusChangeEvent(
                savedOrder.getId(), savedOrder.getStatus(), savedOrder.getClientId(), principal.email()
        ));
    }

    private List<OrderItem> mapToOrderItem(List<ServiceDetailDto> services) {
        return services.stream()
                .map(dto -> {
                    OrderItem item = new OrderItem();
                    item.setServiceId(dto.id());
                    item.setServiceName(dto.name());
                    item.setPriceAtOrder(dto.price());
                    return item;
                })
                .toList();
    }

    public void updateOrderStatus(Long orderId, String targetStatusId, String clientEmail) {
        Order dbOrder = getOrderOrThrow(orderId);
        OrderStatus dbStatus = getStatusOrThrow(targetStatusId);

        dbOrder.setStatus(dbStatus);
        orderRepository.save(dbOrder);

        outboxEventService.saveOrderStatusEvent(new OrderStatusChangeEvent(
                dbOrder.getId(), dbStatus, dbOrder.getClientId(), clientEmail
        ));
        log.info("Статус заказа ID: {} изменен в БД на '{}'", orderId, targetStatusId);
    }

    public void updateOrderDetails(Long orderId, String targetStatusId, boolean isStatusChanged,
                                   String clientEmail, Set<UUID> workerIds, ValidationResponse validationResponse) {
        Order dbOrder = getOrderOrThrow(orderId);

        if (isStatusChanged) {
            OrderStatus newStatus = getStatusOrThrow(targetStatusId);
            dbOrder.setStatus(newStatus);
            outboxEventService.saveOrderStatusEvent(new OrderStatusChangeEvent(
                    dbOrder.getId(), newStatus, dbOrder.getClientId(), clientEmail
            ));
        }

        if (workerIds.isEmpty()) {
            log.info("Удаление мастеров с заказа ID: {}", orderId);
            dbOrder.clearWorkers();
        } else {
            List<WorkerAssignmentEvent> assignmentEvents = dbOrder.replaceWorkers(workerIds, validationResponse.emails());
            outboxEventService.saveWorkerAssignmentEvents(assignmentEvents);
        }

        orderRepository.save(dbOrder);
    }


    private Order getOrderOrThrow(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Заказ с таким id не был найден"));
    }

    private OrderStatus getStatusOrThrow(String id) {
        return orderStatusRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Статус с таким id не был найден"));
    }
}