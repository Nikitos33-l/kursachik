package org.example.kursach.controller;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.example.kursach.dto.Client_OrderDTO;
import org.example.kursach.dto.OrderDTO;
import org.example.kursach.dto.ReguestOrderDTO;
import org.example.kursach.dto.Update_orderDTO;
import org.example.kursach.entity.Order_statuse;
import org.example.kursach.service.JWTService;
import org.example.kursach.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order")
public class OrderController {
    private final OrderService orderService;
    private final JWTService jwtService;

    public OrderController(OrderService orderService, JWTService jwtService) {
        this.orderService = orderService;
        this.jwtService = jwtService;
    }

    @GetMapping("/get/{id}")
    public OrderDTO find(@PathVariable Long id){
        return orderService.find(id);
    }

    @GetMapping("/getAll")
    public List<OrderDTO> findAll(){return orderService.findAll();}

    @GetMapping("/getClientOrder")
    public List<Client_OrderDTO> find_user_order(HttpServletRequest request){
        return orderService.find_user_order(jwtService.get_token(request));
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
    public void create_order(@RequestBody @Valid ReguestOrderDTO order, HttpServletRequest request){
        orderService.create_element(order,jwtService.get_token(request));
    }

}

