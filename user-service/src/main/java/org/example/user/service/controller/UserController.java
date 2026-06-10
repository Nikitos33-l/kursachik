package org.example.user.service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.securitycommon.UserPrincipal;
import org.example.user.api.requestDto.OrderUserMappingRequest;
import org.example.user.api.responceDto.OrderInfoFromUserServiceDto;
import org.example.user.api.responceDto.ValidationResponse;
import org.example.user.service.dto.request.RequestAddUserDto;
import org.example.user.service.dto.request.RequestUpdateUserDto;
import org.example.user.service.dto.response.ResponseUserDto;
import org.example.user.service.dto.response.UserShortResponse;
import org.example.user.service.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
@Tag(name = "Управление пользователями", description = "Операции с профилями сотрудников, клиентов и внутренние межсервисные проверки")
public class UserController {

    private final UserService userService;

    @GetMapping("/getAll")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Получить всех пользователей станции", description = "Доступно только пользователям с ролью ADMIN. Возвращает список пользователей, привязанных к станции текущего администратора.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список успешно получен"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен (недостаточно прав)")
    })
    public List<ResponseUserDto> getAll(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("ADMIN [{}] запросил список всех пользователей для автосервиса ID: {}", userPrincipal.email(), userPrincipal.stationId());
        List<ResponseUserDto> users = userService.getAll(userPrincipal.stationId());
        log.debug("Найдено {} пользователей для автосервиса ID: {}", users.size(), userPrincipal.stationId());
        return users;
    }

    @GetMapping("/get/all/workers")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Получить всех работников станции", description = "Доступно только для ADMIN. Возвращает краткий список сотрудников автосервиса.")
    public List<UserShortResponse> getWorkers(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("ADMIN [{}] запросил список сотрудников для автосервиса ID: {}", userPrincipal.email(), userPrincipal.stationId());
        return userService.getAllWorkers(userPrincipal.stationId());
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Удалить пользователя", description = "Удаляет пользователя системы по его уникальному UUID. Доступно только для ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Пользователь успешно удален"),
            @ApiResponse(responseCode = "404", description = "Пользователь с таким ID не найден")
    })
    public void delete(@Parameter(description = "UUID пользователя", example = "123e4567-e89b-12d3-a456-426614174000") @PathVariable UUID id) {
        log.info("Запрос на удаление пользователя с ID: {}", id);
        userService.deleteUser(id);
        log.info("Пользователь с ID: {} успешно удален", id);
    }

    @GetMapping("/get/info/{id}")
    @Operation(summary = "Получить краткую информацию о пользователе", description = "Возвращает базовые данные пользователя (имя, контакты) по его UUID.")
    public UserShortResponse getInfo(@Parameter(description = "UUID пользователя") @PathVariable UUID id) {
        log.info("Запрос информации о пользователе с ID: {}", id);
        return userService.getInfo(id);
    }

    @PutMapping("/update/{id}")
    @Operation(summary = "Обновить данные пользователя", description = "Позволяет изменить имя и почту существующего профиля.")
    @ApiResponse(responseCode = "200", description = "Данные успешно обновлены")
    public void updateUser(
            @Parameter(description = "UUID пользователя") @PathVariable UUID id,
            @RequestBody @Valid RequestUpdateUserDto userDto
    ) {
        log.info("Запрос на обновление профиля пользователя ID: {}. Новое имя: {}, Email: {}", id, userDto.name(), userDto.email());
        userService.updateUser(id, userDto);
        log.info("Профиль пользователя ID: {} успешно сохранен", id);
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(summary = "Создать нового пользователя", description = "Добавление сотрудника или администратора в систему. Доступно ADMIN и SUPERADMIN.")
    @ApiResponse(responseCode = "200", description = "Пользователь успешно создан")
    public void addUser(
            @RequestBody @Valid RequestAddUserDto userDto,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("Инициатор [{}] создает нового пользователя с email: {} и ролью: {}", userPrincipal.email(), userDto.email(), userDto.role());
        userService.addUser(userDto, userPrincipal);
        log.info("Новый пользователь [{}] успешно добавлен", userDto.email());
    }


    @PostMapping("/internal/get/orderInfo")
    @Operation(summary = "[Внутренний] Инфо о пользователе для заказа", description = "Используется сервисом заказов для сопоставления сущностей.")
    public OrderInfoFromUserServiceDto getOrderInfo(@RequestBody OrderUserMappingRequest request) {
        log.debug("Внутренний запрос getOrderInfo");
        return userService.getInfoForOrder(request);
    }

    @PostMapping("/internal/getAll/orderInfo")
    @Operation(summary = "[Внутренний] Пакетное инфо о пользователях для списка заказов")
    public Map<Long, OrderInfoFromUserServiceDto> getOrdersInfo(@RequestBody List<OrderUserMappingRequest> request) {
        log.debug("Внутренний запрос getOrdersInfo. Размер пакета: {}", request.size());
        return userService.getInfoForOrders(request);
    }

    @PostMapping("/internal/validate-workers")
    @Operation(summary = "[Внутренний] Валидация списка сотрудников", description = "Проверяет существование и активность переданных ID работников.")
    public ValidationResponse validateWorkers(@Parameter(description = "Набор UUID сотрудников") @RequestParam Set<UUID> ids) {
        log.debug("Внутренняя валидация списка сотрудников. Количество ID на проверку: {}", ids.size());
        return userService.validateWorkers(ids);
    }

    @GetMapping("/internal/email/{id}")
    @Operation(summary = "[Внутренний] Получить email по ID", description = "Используется сервисом уведомлений для отправки сообщений.")
    public String getEmailByUserId(@Parameter(description = "UUID пользователя") @PathVariable UUID id) {
        log.debug("Внутренний запрос email для пользователя ID: {}", id);
        return userService.getEmailById(id);
    }
}