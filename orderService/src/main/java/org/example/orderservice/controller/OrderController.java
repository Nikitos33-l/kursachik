package org.example.orderservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.orderservice.dto.response.ResponseOrderDto;
import org.example.orderservice.service.OrderManagementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderManagementService orderService;

    @GetMapping("/get/{id}")
    public ResponseOrderDto find(@PathVariable Long id){
        return orderService.find(id);
    }
}
