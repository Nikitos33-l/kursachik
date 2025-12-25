package org.example.kursach.mapping;


import org.example.kursach.dto.Client_OrderDTO;
import org.example.kursach.entity.Order;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class Client_OrderDTO_map implements Function<Order, Client_OrderDTO> {


    @Override
    public Client_OrderDTO apply(Order order) {
        return new Client_OrderDTO(order.getVenicle(),order.getServices(),order.getStatuse().getName());
    }
}
