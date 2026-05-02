package org.example.station.service.api.common.client;

import org.example.station.service.api.common.dto.request.RequestOrderMappingStationDto;
import org.example.station.service.api.common.dto.response.ResponseStationDto;
import org.example.station.service.api.common.dto.response.StationServicesResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "station-service")
public interface StationServiceClient {

    @GetMapping("api/stations/get/station/by/order/{id}")
    ResponseStationDto getStationByOrderId(@PathVariable Long orderId);

    @GetMapping("api/stations/getAll/by/station")
    Map<Long,ResponseStationDto> getStationsByOrders(@RequestBody List<RequestOrderMappingStationDto> request);

    @PostMapping("/api/stations/{stationId}/validate-services")
    StationServicesResponse validateStationServices(
            @PathVariable Long stationId,
            @RequestBody List<Long> serviceIds
    );
}
