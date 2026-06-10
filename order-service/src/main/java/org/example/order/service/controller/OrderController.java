package org.example.order.service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.service.dto.request.PutOrderRequestDto;
import org.example.order.service.dto.request.RequestOrderDto;
import org.example.order.service.dto.request.RequestOrderStatusDto;
import org.example.order.service.dto.response.ResponseOrderDto;
import org.example.order.service.dto.response.ResponseOrderSummaryDto;
import org.example.order.service.service.OrderManagementService;
import org.example.securitycommon.UserPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Tag(name = "Управление заказами СТО", description = "Операции по созданию, обновлению статусов и распределению заказов между автомеханиками")
public class OrderController {

    private final OrderManagementService orderService;

    @GetMapping("/get/{id}")
    @Operation(summary = "Получить детальную информацию о заказе по ID")
    public ResponseOrderDto find(@PathVariable Long id) {
        log.debug("Запрос информации о заказе с ID: {}", id);
        return orderService.find(id);
    }

    @PutMapping("/updateStatus/{id}")
    @PreAuthorize("hasRole('WORKER')")
    @Operation(summary = "Обновить статус выполнения заказа", description = "Доступно только автомеханикам (WORKER) для изменения этапов ремонта")
    public void updateOrderStatus(
            @PathVariable Long id,
            @RequestBody RequestOrderStatusDto status,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("Мастер [{}] затребовал смену статуса заказа ID {} на '{}'", userPrincipal.email(), id, status.id());
        orderService.updateStatus(id, status);
        log.info("Статус заказа ID {} успешно обновлен", id);
    }

    @GetMapping("/getAll")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Получить все заказы привязанной станции", description = "Доступно администратору СТО (ADMIN). Фильтрует заказы по его stationId")
    public List<ResponseOrderDto> findAll(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Администратор [{}] запросил список всех заказов для СТО ID: {}", userPrincipal.email(), userPrincipal.stationId());
        return orderService.findAll(userPrincipal.stationId());
    }

    @GetMapping("/getClientOrder")
    @Operation(summary = "Получить историю заказов текущего клиента", description = "Возвращает список кратких сущностей заказов авторизованного пользователя")
    public List<ResponseOrderSummaryDto> findUserOrder(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.debug("Клиент [{}] запросил историю своих заказов", userPrincipal.email());
        return orderService.findUserOrder(userPrincipal);
    }

    @PutMapping("/updateOrder/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Полное редактирование заказа администратором", description = "Позволяет переназначить список исполнителей (механиков) и скорректировать статус")
    public void updateOrder(
            @PathVariable Long id,
            @RequestBody PutOrderRequestDto requestDto,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("Администратор [{}] обновляет параметры заказа ID: {}. Назначаемые мастера: {}, Новый статус: {}",
                userPrincipal.email(), id, requestDto.workersId(), requestDto.statusId());
        orderService.updateOrder(requestDto, id);
        log.info("Заказ ID {} успешно отредактирован администратором", id);
    }

    @PostMapping("/create")
    @Operation(summary = "Оформить новую заявку на ремонт (Создать заказ)")
    public void createOrder(
            @RequestBody RequestOrderDto order,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("Инициация создания заказа клиентом [{}]. СТО ID: {}, Выбранные услуги ID: {}",
                userPrincipal.email(), order.stationId(), order.serviceId());
        orderService.createOrder(order, userPrincipal);
        log.info("Заказ для клиента [{}] успешно сформирован в системе", userPrincipal.email());
    }

    @GetMapping("/getWorkerOrder")
    @PreAuthorize("hasRole('WORKER')")
    @Operation(summary = "Получить список активных задач текущего мастера", description = "Возвращает заказы, в которых авторизованный WORKER назначен исполнителем")
    public List<ResponseOrderDto> findWorkerOrders(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.debug("Мастер [{}] запросил список назначенных на него заказов", userPrincipal.email());
        return orderService.findWorkerOrder(userPrincipal);
    }
}