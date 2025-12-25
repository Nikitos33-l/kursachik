package org.example.kursach.controller;


import org.example.kursach.entity.Order_statuse;
import org.example.kursach.service.Order_statusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/api/status/order")
@RestController
public class Order_statusController {
    private final Order_statusService order_statusService;

    public Order_statusController(Order_statusService orderStatusService) {
        order_statusService = orderStatusService;
    }

    @GetMapping("/getAll")
    public List<Order_statuse> get_all(){
        return order_statusService.get_all();
    }
}
