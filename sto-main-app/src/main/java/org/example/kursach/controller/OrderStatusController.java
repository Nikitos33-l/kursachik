package org.example.kursach.controller;


import org.example.kursach.entity.OrderStatus;
import org.example.kursach.service.OrderStatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/api/status/order")
@RestController
public class OrderStatusController {
    private final OrderStatusService order_statusService;

    public OrderStatusController(OrderStatusService orderStatusService) {
        order_statusService = orderStatusService;
    }

    @GetMapping("/getAll")
    public List<OrderStatus> getAll(){
        return order_statusService.getAll();
    }
}
