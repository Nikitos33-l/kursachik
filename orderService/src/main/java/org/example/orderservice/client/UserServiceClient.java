package org.example.orderservice.client;

import org.example.orderservice.dto.external.UserDto;
import org.example.orderservice.dto.external.VehicleDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Set;

@FeignClient(name = "userService")
public interface UserServiceClient {
    @GetMapping("/api/user/findByIds")
    List<UserDto> findByIds(@RequestParam Set<Long> ids);

    @GetMapping("/api/user/findById/{id}")
    UserDto findById(@PathVariable Long id);

    @GetMapping("/api/user/findCar/{id}")
    VehicleDto findCar(@PathVariable Long id);
}
