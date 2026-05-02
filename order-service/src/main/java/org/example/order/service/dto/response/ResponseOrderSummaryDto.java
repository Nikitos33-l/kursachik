package org.example.order.service.dto.response;

import org.example.user.api.responceDto.VehicleDto;

import java.util.List;

public record ResponseOrderSummaryDto(
        VehicleDto vehicle,
        List<OrderItemDto>services,
        String status,
        String stationName
) {
}
