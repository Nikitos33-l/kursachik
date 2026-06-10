package org.example.order.service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Данные для оформления новой заявки на ремонт")
public record RequestOrderDto(
        @Schema(description = "Информация о транспортном средстве")
        RequestVehicleDto vehicle,

        @Schema(description = "Список ID выбранных услуг автосервиса", example = "[1, 3, 14]")
        List<Long> serviceId,

        @Schema(description = "ID станции (филиала СТО), на которую оформляется запись", example = "2")
        Long stationId
) {
}