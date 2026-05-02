package org.example.orderservice.mapper;

import org.example.orderservice.dto.response.OrderItemDto;
import org.example.orderservice.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderItemMapper {

    @Mapping(target = "id", source = "serviceId")
    @Mapping(target = "name", source = "serviceName")
    @Mapping(target = "price", source = "priceAtOrder")
    OrderItemDto toDto(OrderItem orderItem);
}
