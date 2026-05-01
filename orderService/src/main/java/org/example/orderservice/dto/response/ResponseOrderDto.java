package org.example.orderservice.dto.response;

import org.example.orderservice.dto.external.UserDto;
import org.example.orderservice.dto.external.VehicleDto;

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
