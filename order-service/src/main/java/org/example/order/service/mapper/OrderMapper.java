package org.example.order.service.mapper;

import org.example.order.service.dto.response.OrderItemDto;
import org.example.order.service.dto.response.ResponseOrderDto;
import org.example.order.service.entity.Order;
import org.example.user.api.responceDto.OrderInfoFromUserServiceDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderMapper {

    @Mapping(target = "id", source = "order.id")
    @Mapping(target = "status", source = "order.status.name")
    @Mapping(target = "vehicle", source = "response.vehicle")
    @Mapping(target = "client", source = "response.client")
    @Mapping(target = "workers", source = "response.workers")
    @Mapping(target = "services", source = "orderItem")
    ResponseOrderDto toResponseOrderDto(
            Order order,
            OrderInfoFromUserServiceDto response,
            List<OrderItemDto> orderItem
    );

}
