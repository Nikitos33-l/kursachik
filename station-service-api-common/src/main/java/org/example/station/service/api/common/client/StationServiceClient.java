package org.example.station.service.api.common.client;

import org.example.station.service.api.common.dto.request.RequestOrderMappingStationDto;
import org.example.station.service.api.common.dto.response.SummaryResponseStationDto;
import org.example.station.service.api.common.dto.response.StationServicesResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "station-service")
public interface StationServiceClient {

    @GetMapping("/api/stations/getAll/by/station")
    Map<Long, SummaryResponseStationDto> getStationsByOrders(@RequestBody List<RequestOrderMappingStationDto> request);

    @GetMapping("/api/service/{stationId}/validate")
    StationServicesResponse validateStationAndGetServices(
            @PathVariable Long stationId,
            @RequestBody List<Long> serviceIds
    );
}
