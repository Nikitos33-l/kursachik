package org.example.order.service.dto.request;

import java.util.List;

public record RequestOrderDto(
        RequestVehicleDto vehicle,
        List<Long> serviceId,
        Long stationId
) {
}
