package org.example.kursach.mapping;


import org.example.kursach.dto.ClientOrderDTO;
import org.example.kursach.entity.Order;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class ClientOrderDTOMap implements Function<Order, ClientOrderDTO> {

    @Override
    public ClientOrderDTO apply(Order order) {
        String stationName = order.getStation() != null ? order.getStation().getName() : "Не указано";
        return new ClientOrderDTO(order.getVehicle(),order.getServices(),order.getStatuse().getName(),stationName);
    }
}
