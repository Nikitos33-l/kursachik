package org.example.user.api.client;

import org.example.user.api.requestDto.CarRequestDto;
import org.example.user.api.requestDto.OrderUserMappingRequest;
import org.example.user.api.requestDto.OrderVehicleMappingRequest;
import org.example.user.api.responceDto.OrderInfoFromUserServiceDto;
import org.example.user.api.responceDto.ValidationResponse;
import org.example.user.api.responceDto.VehicleDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.Set;

@FeignClient(name = "userService")
public interface UserServiceFeignClient {

    @GetMapping("api/users/get/orderInfo")
    OrderInfoFromUserServiceDto getOrderInfo(@RequestBody OrderUserMappingRequest request);

    @GetMapping("api/users/getAll/order")
    Map<Long,OrderInfoFromUserServiceDto> getOrdersInfo(@RequestBody List<OrderUserMappingRequest> request);

    @GetMapping("api/users/getAll/cars")
    Map<Long, VehicleDto> getCarsInfo(@RequestBody List<OrderVehicleMappingRequest> request);

    @GetMapping("/api/users/validate-workers")
    ValidationResponse validateWorkers(@RequestParam Set<Long> ids);

    @PostMapping("/api/cars/get-or-create")
    VehicleDto getOrCreateCar(@RequestBody CarRequestDto carRequest);


}
