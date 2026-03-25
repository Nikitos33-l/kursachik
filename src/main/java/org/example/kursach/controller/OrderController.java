package org.example.kursach.controller;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.example.kursach.dto.*;
import org.example.kursach.entity.Order_statuse;
import org.example.kursach.service.JWTService;
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
    public List<OrderDTO> findAll(){return orderService.findAll();}

    @GetMapping("/getClientOrder")
    public List<Client_OrderDTO> find_user_order(@AuthenticationPrincipal UserPrincipal userPrincipal){
        return orderService.find_user_order(userPrincipal);
    }

    @PutMapping("/update_status/{id}")
    @PreAuthorize("hasRole('WORKER')")
    public void update_Order_status(@PathVariable Long id, @RequestBody Order_statuse status){
         orderService.update_status(id, status);
    }

    @PutMapping("/update_order/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void update_order(@PathVariable Long id, @RequestBody @Valid Update_orderDTO new_order){
        orderService.update_order(new_order,id);
    }

    @PostMapping("/create")
    public void create_order(@RequestBody @Valid ReguestOrderDTO order, @AuthenticationPrincipal UserPrincipal userPrincipal){
        orderService.create_element(order,userPrincipal);
    }

}

