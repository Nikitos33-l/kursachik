package org.example.orderservice.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.orderservice.dto.response.ResponseOrderDto;
import org.example.orderservice.entity.Order;
import org.example.orderservice.repository.OrderRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderManagementService {

    private final OrderRepository orderRepository;

    public ResponseOrderDto find(Long id) {
        Order dbOrder = orderRepository.findById(id).orElseThrow(()-> new EntityNotFoundException("Заказ с таким id не был найден"));

        return null;
    }
}
