package org.example.order.service.controller;

import lombok.RequiredArgsConstructor;
import org.example.order.service.dto.request.RequestOrderStatusDto;
import org.example.order.service.dto.response.ResponseOrderDto;
import org.example.order.service.dto.response.ResponseOrderSummaryDto;
import org.example.order.service.service.OrderManagementService;
import org.example.securitycommon.UserPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/getAll")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ResponseOrderDto> findAll(@AuthenticationPrincipal UserPrincipal userPrincipal){
        return orderService.findAll(userPrincipal.stationId());
    }

    @GetMapping("/getClientOrder")
    public List<ResponseOrderSummaryDto> findUserOrder(@AuthenticationPrincipal UserPrincipal userPrincipal){
        return orderService.findUserOrder(userPrincipal);
    }



}
