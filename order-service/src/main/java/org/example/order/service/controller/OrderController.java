package org.example.order.service.controller;

import lombok.RequiredArgsConstructor;
import org.example.order.service.dto.request.PutOrderRequestDto;
import org.example.order.service.dto.request.RequestOrderDto;
import org.example.order.service.dto.request.RequestOrderStatusDto;
import org.example.order.service.dto.response.ResponseOrderDto;
import org.example.order.service.dto.response.ResponseOrderSummaryDto;
import org.example.order.service.service.OrderManagementService;
import org.example.securitycommon.UserPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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

    @PutMapping("/updateOrder/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void updateOrder(@PathVariable Long orderId, @RequestBody PutOrderRequestDto requestDto){
        orderService.updateOrder(requestDto,orderId);
    }

    @PostMapping("/create")
    public void create_order(@RequestBody RequestOrderDto order, @AuthenticationPrincipal UserPrincipal userPrincipal){
        orderService.createOrder(order,userPrincipal);
    }

    @GetMapping("/getWorkerOrder")
    @PreAuthorize("hasRole('WORKER')")
    public List<ResponseOrderDto> findWorkerOrders(@AuthenticationPrincipal UserPrincipal userPrincipal){
        return orderService.findWorkerOrder(userPrincipal);
    }

    @DeleteMapping("/internal/delete/by/stationId/{id}")
    public void deleteByStation(@PathVariable Long id){
        orderService.deleteByOrder(id);
    }
}
