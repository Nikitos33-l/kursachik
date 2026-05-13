package org.example.user.service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.securitycommon.UserPrincipal;
import org.example.user.api.requestDto.OrderUserMappingRequest;
import org.example.user.api.requestDto.OrderVehicleMappingRequest;
import org.example.user.api.responceDto.OrderInfoFromUserServiceDto;
import org.example.user.api.responceDto.ValidationResponse;
import org.example.user.api.responceDto.VehicleDto;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;

    @GetMapping("/getAll")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ResponseUserDto> getAll(@AuthenticationPrincipal UserPrincipal userPrincipal){
        return userService.getAll(userPrincipal.stationId());
    }

    @GetMapping("/get/all/workers")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserShortResponse> getWorkers(@AuthenticationPrincipal UserPrincipal userPrincipal){
        return userService.getAllWorkers(userPrincipal.stationId());
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id){
        userService.deleteUser(id);
    }

    @GetMapping("/get/info/{id}")
    public UserShortResponse getInfo(@PathVariable Long id){
        return userService.getInfo(id);
    }

    @PutMapping("/update/{id}")
    public void updateUser(@PathVariable Long id, @RequestBody @Valid RequestUpdateUserDto userDto){
        userService.updateUser(id,userDto);
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public void addUser(@RequestBody @Valid RequestAddUserDto userDto,@AuthenticationPrincipal UserPrincipal userPrincipal){
        userService.addUser(userDto,userPrincipal);
    }

    @GetMapping("/get/orderInfo")
    public OrderInfoFromUserServiceDto getOrderInfo(@RequestBody OrderUserMappingRequest request){
        return userService.getInfoForOrder(request);
    }

    @GetMapping("/getAll/order")
    public Map<Long,OrderInfoFromUserServiceDto> getOrdersInfo(@RequestBody List<OrderUserMappingRequest> request){
        return userService.getInfoForOrders(request);
    }

    @GetMapping("/validate-workers")
    public ValidationResponse validateWorkers(@RequestParam Set<Long> ids){
        return userService.validateWorkers(ids);
    }

    @DeleteMapping("/delete/by/workplace/{id}")
    public void deleteWorkersByWorkplace(@PathVariable Long id){
        userService.deleteByWorkplace(id);
    }
}
