package org.example.user.api.client;

import org.example.user.api.requestDto.OrderUserMappingRequest;
import org.example.user.api.requestDto.OrderVehicleMappingRequest;
import org.example.user.api.responceDto.OrderInfoFromUserServiceDto;
import org.example.user.api.responceDto.VehicleDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "userService")
public interface UserServiceFeignClient {

    @GetMapping("/get/orderInfo")
    OrderInfoFromUserServiceDto getOrderInfo(@RequestBody OrderUserMappingRequest request);

    @GetMapping("/getAll/order")
    Map<Long,OrderInfoFromUserServiceDto> getOrdersInfo(@RequestBody List<OrderUserMappingRequest> request);

    @GetMapping("/getAll/cars")
    Map<Long, VehicleDto> getCarsInfo(@RequestBody List<OrderVehicleMappingRequest> request);


}
