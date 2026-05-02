package org.example.order.service.dto.request;

import org.example.user.api.responceDto.VehicleDto;

import java.util.List;

public record RequestOrderDto(
        RequestVehicleDto vehicle,
        List<Long> serviceId,
        Long stationId
) {
}
