package org.example.kursach.service;


import org.example.kursach.entity.OrderStatus;
import org.example.kursach.repository.OrderStatusRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderStatusService {
    OrderStatusRepository orderStatusRepository;

    public OrderStatusService(OrderStatusRepository orderStatusRepository) {
        this.orderStatusRepository = orderStatusRepository;
    }

    public List<OrderStatus> getAll(){
        return  orderStatusRepository.findAll();
    }

}
