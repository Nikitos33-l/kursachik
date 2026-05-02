package org.example.orderservice.mapper;

import org.example.orderservice.dto.external.UserDto;
import org.example.orderservice.dto.external.VehicleDto;
import org.example.orderservice.dto.response.OrderItemDto;
import org.example.orderservice.dto.response.ResponseOrderDto;
import org.example.orderservice.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderMapper {

    @Mapping(target = "id", source = "order.id")
    @Mapping(target = "status", source = "order.status.name")
    @Mapping(target = "vehicle", source = "vehicleDto")
    @Mapping(target = "client", source = "client")
    @Mapping(target = "workers", source = "workers")
    @Mapping(target = "services", source = "orderItem")
    ResponseOrderDto toResponseOrderDto(
            Order order,
            VehicleDto vehicleDto,
            UserDto client,
            List<UserDto> workers,
            List<OrderItemDto> orderItem
    );
}
