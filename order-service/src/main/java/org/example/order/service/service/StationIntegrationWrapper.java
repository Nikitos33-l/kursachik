package org.example.order.service.service;

import lombok.RequiredArgsConstructor;
import org.example.station.service.api.common.client.StationServiceClient;
import org.example.station.service.api.common.dto.response.StationServicesResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StationIntegrationWrapper {

    private final StationServiceClient stationClient;

    @Cacheable(
            value = "order-service:station-validation",
            key = "'station:' + #stationId + ':services:' + #serviceIds.stream().sorted().toList().toString()"
    )
    public StationServicesResponse getValidatedServices(Long stationId, List<Long> serviceIds) {
        return stationClient.validateStationAndGetServices(stationId, serviceIds);
    }
}
