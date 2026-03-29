package org.example.kursach.service;

import org.example.kursach.entity.Order;
import org.example.kursach.entity.OrderStatus;
import org.example.kursach.repository.OrderRepository;
import org.example.kursach.repository.StatusRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    OrderRepository orderRepository;

    @Mock
    StatusRepository statusRepository;

    @InjectMocks
    OrderService orderService;

    @Test
    @DisplayName("Успешное обновление статуса заказа")
    public void successful_update_status_test(){
        Long id = 1L;
        OrderStatus orderStatuse = create_orderStatuse("DONE","Выполнен");
        Order order = create_order();

        when(orderRepository.findById(id)).thenReturn(Optional.of(order));
        when(statusRepository.findById("DONE")).thenReturn(Optional.of(orderStatuse));

        orderService.updateStatus(id,orderStatuse);

        assertEquals(orderStatuse,order.getStatuse());

        verify(orderRepository,times(1)).findById(id);
        verify(statusRepository,times(1)).findById("DONE");
    }

    public Order create_order(){
        Order order = new Order();
        order.setId(1L);
        order.setStatuse(create_orderStatuse("NEW","Новый"));
        return order;
    }

    public OrderStatus create_orderStatuse(String id, String name){
        OrderStatus orderStatuse = new OrderStatus();
        orderStatuse.setId(id);
        orderStatuse.setName(name);
        return orderStatuse;
    }


}
