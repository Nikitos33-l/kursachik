package org.example.station.service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.station.service.constants.CacheNames;
import org.example.station.service.dto.response.ResponseServiceDto;
import org.example.station.service.mapper.ServiceMapper;
import org.example.station.service.repository.ServiceRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceCacheRepository {

    private final ServiceRepository serviceRepository;
    private final ServiceMapper serviceMapper;

    @Cacheable(value = CacheNames.SERVICE_CACHE + "stationId:", key = "#stationId")
    @Transactional(readOnly = true)
    public List<ResponseServiceDto> getServicesByStationId(Long stationId) {
        log.debug("[CACHE MISS] Считывание списка услуг для СТО ID: {} напрямую из БД", stationId);
        return serviceMapper.toDtoList(serviceRepository.findAllByStation_id(stationId));
    }

    @CacheEvict(value = CacheNames.SERVICE_CACHE + "stationId:", key = "#stationId")
    public void evictCache(Long stationId) {
        log.info("[CACHE EVICT] Инвалидация кэша услуг для СТО ID: {}", stationId);
    }
}