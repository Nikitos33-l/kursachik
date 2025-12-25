package org.example.kursach.service;


import org.example.kursach.entity.Order_statuse;
import org.example.kursach.repository.Order_statusRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class Order_statusService {
    Order_statusRepository orderStatusRepository;

    public Order_statusService(Order_statusRepository orderStatusRepository) {
        this.orderStatusRepository = orderStatusRepository;
    }

    public List<Order_statuse> get_all(){
        return  orderStatusRepository.findAll();
    }

}
