package org.example.user.api.client;

import org.example.user.api.requestDto.CarRequestDto;
import org.example.user.api.requestDto.OrderUserMappingRequest;
import org.example.user.api.requestDto.OrderVehicleMappingRequest;
import org.example.user.api.responceDto.OrderInfoFromUserServiceDto;
import org.example.user.api.responceDto.ValidationResponse;
import org.example.user.api.responceDto.VehicleDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@FeignClient(name = "user-service")
public interface UserServiceFeignClient {

    @GetMapping("api/user/internal/get/orderInfo")
    OrderInfoFromUserServiceDto getOrderInfo(@RequestBody OrderUserMappingRequest request);

    @GetMapping("api/user/internal/getAll/order")
    Map<Long,OrderInfoFromUserServiceDto> getOrdersInfo(@RequestBody List<OrderUserMappingRequest> request);

    @GetMapping("api/cars/internal/getAll")
    Map<Long, VehicleDto> getCarsInfo(@RequestBody List<OrderVehicleMappingRequest> request);

    @GetMapping("/api/user/internal/validate-workers")
    ValidationResponse validateWorkers(@RequestParam Set<UUID> ids);

    @PostMapping("/api/cars/internal/get-or-create")
    VehicleDto getOrCreateCar(@RequestBody CarRequestDto carRequest);

}
