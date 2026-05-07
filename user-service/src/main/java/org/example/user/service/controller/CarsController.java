package org.example.user.service.controller;

import lombok.RequiredArgsConstructor;
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
public class CarsController {
    private final CarService carService;

    @GetMapping("/getAll")
    Map<Long, VehicleDto> getCarsInfo(@RequestBody List<OrderVehicleMappingRequest> request){
        return carService.getVehiclesForOrders(request);
    }

    @PostMapping("/api/cars/get-or-create")
    VehicleDto getOrCreateCar(@RequestBody CarRequestDto carRequest){
        return carService.getOrCreateCar(carRequest);
    }


}
