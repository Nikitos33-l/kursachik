package org.example.station.service.service;

import lombok.RequiredArgsConstructor;
import org.example.station.service.constants.CacheNames;
import org.example.station.service.dto.response.ResponseServiceDto;
import org.example.station.service.mapper.ServiceMapper;
import org.example.station.service.repository.ServiceRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ServiceCacheRepository {

    private final ServiceRepository serviceRepository;
    private final ServiceMapper serviceMapper;

    @Cacheable(value = CacheNames.SERVICE_CACHE+"stationId:", key = "#stationId")
    @Transactional(readOnly = true)
    public List<ResponseServiceDto> getServicesByStationId(Long stationId) {
        return serviceMapper.toDtoList(serviceRepository.findAllByStation_id(stationId));
    }

    @CacheEvict(value = CacheNames.SERVICE_CACHE+"stationId:", key = "#stationId")
    public void evictCache(Long stationId) {
    }
}
