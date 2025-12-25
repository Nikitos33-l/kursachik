package org.example.kursach.mapping;


import org.example.kursach.dto.OrderDTO;
import org.example.kursach.entity.Order;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class OrderDTO_Map implements Function<Order, OrderDTO> {

    @Override
    public OrderDTO apply(Order order) {
        UserDTO_Map userDTO_map=new UserDTO_Map();
        return new OrderDTO(order.getId(),order.getVenicle(),
                order.getStatuse().getName(),
                userDTO_map.apply(order.getClient()),
                order.getWorkers().
                        stream().
                        map(userDTO_map).toList(),
                order.getServices());
    }
}
