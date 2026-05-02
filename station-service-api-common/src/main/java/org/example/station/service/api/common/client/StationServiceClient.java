package org.example.station.service.api.common.client;

import org.example.station.service.api.common.dto.response.ResponseStationDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "station-service")
public interface StationServiceClient {

    @GetMapping("get/station/by/order/{id}")
    ResponseStationDto getStationByOrderId(@PathVariable Long orderId);
}
