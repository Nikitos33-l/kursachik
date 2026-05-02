package org.example.station.service.api.common.dto.response;

import java.util.List;

public record StationServicesResponse(
        boolean stationExists,
        List<ServiceDetailDto> services
) {
}
