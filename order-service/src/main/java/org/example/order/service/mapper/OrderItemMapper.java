package org.example.order.service.mapper;

import org.example.order.service.dto.response.OrderItemDto;
import org.example.order.service.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderItemMapper {

    @Mapping(target = "id", source = "serviceId")
    @Mapping(target = "name", source = "serviceName")
    @Mapping(target = "price", source = "priceAtOrder")
    OrderItemDto toDto(OrderItem orderItem);

    List<OrderItemDto> toDtoList(List<OrderItem> orderItemList);
}
