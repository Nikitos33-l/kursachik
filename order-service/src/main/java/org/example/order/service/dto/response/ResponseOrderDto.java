package org.example.order.service.dto.response;

import org.example.user.api.responceDto.UserDto;
import org.example.user.api.responceDto.VehicleDto;

import java.util.List;

public record ResponseOrderDto(
    Long id,
    String status,
    VehicleDto vehicle,
    UserDto client,
    List<UserDto> workers,
    List<OrderItemDto> services
)
{}
