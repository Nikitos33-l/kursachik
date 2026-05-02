package org.example.orderservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.orderservice.dto.request.RequestOrderStatusDto;
import org.example.orderservice.dto.response.ResponseOrderDto;
import org.example.orderservice.entity.OrderStatus;
import org.example.orderservice.service.OrderManagementService;
import org.example.securitycommon.UserPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderManagementService orderService;

    @GetMapping("/get/{id}")
    public ResponseOrderDto find(@PathVariable Long id){
        return orderService.find(id);
    }

    @PutMapping("/updateStatus/{id}")
    @PreAuthorize("hasRole('WORKER')")
    public void updateOrderStatus(@PathVariable Long id, @RequestBody RequestOrderStatusDto status){
        orderService.updateStatus(id, status);
    }

}
