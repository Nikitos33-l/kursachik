package org.example.user.service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.user.api.requestDto.CarRequestDto;
import org.example.user.api.requestDto.OrderVehicleMappingRequest;
import org.example.user.api.responceDto.VehicleDto;
import org.example.user.service.service.CarService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cars")
@Tag(name = "Управление автомобилями",description = "Внутренние операции для ответа другим сервисам информацией об автомобилях")
@Slf4j
public class CarsController {
    private final CarService carService;

    @PostMapping("/internal/getAll")
    @Operation(summary = "[Внутренний] получить информацию об автомобилях",description = "Используется сервисом заказов для получения информации об автомобиле в заказе")
    Map<Long, VehicleDto> getCarsInfo(@RequestBody List<OrderVehicleMappingRequest> request){
        log.info("Получен запрос от сервиса заказов на получение информации об автомобилях");
        return carService.getVehiclesForOrders(request);
    }

    @PostMapping("/internal/get-or-create")
    @Operation(summary = "[Внутренний] получить информацию об автомобиле или создать его",description = "Используется сервисом заказов при имеющемся автомобиле просто возвращает информацию о нем,в противном случае создает его и отдает информацию о нем")
    VehicleDto getOrCreateCar(@RequestBody CarRequestDto carRequest){
        log.info("Запрос от сервиса заказов на получение информации или создание автомобиля");
        return carService.getOrCreateCar(carRequest);
    }


}
