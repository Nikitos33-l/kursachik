package org.example.kursach.controller;


import jakarta.validation.Valid;
import org.example.kursach.dto.*;
import org.example.kursach.entity.OrderStatus;
import org.example.kursach.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/get/{id}")
    public OrderDTO find(@PathVariable Long id){
        return orderService.find(id);
    }

    @GetMapping("/getAll")
    @PreAuthorize("hasRole('ADMIN')")
    public List<OrderDTO> findAll(@AuthenticationPrincipal UserPrincipal userPrincipal){
        return orderService.findAll(userPrincipal.stationId());
    }

    @GetMapping("/getClientOrder")
    public List<ClientOrderDTO> findUserOrder(@AuthenticationPrincipal UserPrincipal userPrincipal){
        return orderService.findUserOrder(userPrincipal);
    }

    @PutMapping("/updateStatus/{id}")
    @PreAuthorize("hasRole('WORKER')")
    public void updateOrderStatus(@PathVariable Long id, @RequestBody OrderStatus status){
         orderService.updateStatus(id, status);
    }

    @PutMapping("/updateOrder/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void updateOrder(@PathVariable Long id, @RequestBody @Valid Update_orderDTO new_order){
        orderService.updateOrder(new_order,id);
    }

    @PostMapping("/create")
    public void create_order(@RequestBody @Valid ReguestOrderDTO order, @AuthenticationPrincipal UserPrincipal userPrincipal){
        orderService.createElement(order,userPrincipal);
    }

}

