package org.example.station.service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.securitycommon.UserPrincipal;
import org.example.station.service.api.common.dto.response.StationServicesResponse;
import org.example.station.service.dto.request.RequestServiceDto;
import org.example.station.service.dto.response.ResponseServiceDto;
import org.example.station.service.service.ServiceManagementService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/service")
@RequiredArgsConstructor
@Tag(name = "Управление услугами СТО", description = "Операции с прайс-листом и перечнем услуг автосервисов, включая внутренние проверки")
public class ServiceController {
    private final ServiceManagementService service;

    @GetMapping({"/getAll", "/getAll/{stationId}"})
    @Operation(summary = "Получить список всех услуг", description = "Возвращает перечень услуг. Если stationId не указан, берется stationId из токена текущего пользователя.")
    public List<ResponseServiceDto> getAll(
            @Parameter(description = "ID станции (опционально)") @PathVariable(required = false) Long stationId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("Запрос списка услуг. Инициатор: [{}], переданный stationId: {}", userPrincipal.email(), stationId);
        List<ResponseServiceDto> services = service.findAll(stationId, userPrincipal);
        log.debug("Возвращено {} услуг", services.size());
        return services;
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "Получить услугу по ID", description = "Возвращает детальную информацию о конкретной услуге.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Услуга успешно найдена"),
            @ApiResponse(responseCode = "404", description = "Услуга с таким ID не существует")
    })
    public ResponseServiceDto getById(@Parameter(description = "ID услуги", example = "1") @PathVariable Long id) {
        log.debug("Запрос информации об услуге с ID: {}", id);
        return service.findById(id);
    }

    @DeleteMapping("/del/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Удалить услугу", description = "Доступно только для ADMIN. Удаляет услугу из системы по ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Услуга успешно удалена"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен (требуется роль ADMIN)")
    })
    public void delete(@Parameter(description = "ID удаляемой услуги") @PathVariable Long id) {
        log.info("Запрос на удаление услуги ID: {}", id);
        service.delete(id);
        log.info("Услуга ID: {} успешно удалена", id);
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Добавить новую услугу", description = "Доступно только для ADMIN. Создает новую позицию в прайс-листе СТО.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Услуга успешно создана"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен")
    })
    public void add(
            @RequestBody @Valid RequestServiceDto serviceDto,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("ADMIN [{}] инициировал создание услуги: '{}'", userPrincipal.email(), serviceDto.name());
        service.add(serviceDto, userPrincipal);
        log.info("Услуга '{}' успешно добавлена в систему", serviceDto.name());
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','WORKER')")
    @Operation(summary = "Обновить данные услуги", description = "Доступно для ADMIN и WORKER. Позволяет изменить название, цену или описание услуги.")
    public void update(
            @Parameter(description = "ID редактируемой услуги") @PathVariable Long id,
            @RequestBody @Valid RequestServiceDto serviceDto
    ) {
        log.info("Запрос на обновление услуги ID: {}. Новые параметры -> Название: '{}'", id, serviceDto.name());
        service.update(id, serviceDto);
        log.info("Услуга ID: {} успешно обновлена", id);
    }

    @PostMapping("/internal/{stationId}/validate")
    @Operation(summary = "[Внутренний] Валидация услуг станции", description = "Используется другими микросервисами для проверки соответствия списка услуг конкретной СТО.")
    public StationServicesResponse validateStationAndGetServices(
            @Parameter(description = "ID проверяемой СТО") @PathVariable Long stationId,
            @RequestBody List<Long> serviceIds
    ) {
        log.info("Внутренний запрос валидации услуг для СТО ID: {}. Передано на проверку ID услуг: {}", stationId, serviceIds);
        return service.findByIdsAndValidateService(serviceIds, stationId);
    }
}