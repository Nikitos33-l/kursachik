package org.example.kursach.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.kursach.dto.ServiceRequestDto;
import org.example.kursach.dto.UserPrincipal;
import org.example.kursach.entity.Service;
import org.example.kursach.entity.Stations;
import org.example.kursach.mapping.ServiceMap;
import org.example.kursach.repository.ServiceRepository;
import org.example.kursach.repository.StationsRepository;
import org.springframework.cache.annotation.*;
import java.util.List;

@org.springframework.stereotype.Service
@CacheConfig(cacheNames = {"services"})
@RequiredArgsConstructor
public class ServiceService {
    private final ServiceRepository serviceRepository;
    private final ServiceMap serviceMap;
    private final StationsRepository stationsRepository;

    @Cacheable(key = "#stationId")
    public List<Service> findAll(Long stationId){
        return serviceRepository.findByStationId(stationId);
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "services",allEntries = true),
        @CacheEvict(cacheNames = "serviceDetails",key = "#id")
    })
    @Transactional
    public void delete(Long id){
        if (!serviceRepository.existsById(id)){
             throw new EntityNotFoundException("Объект не найден");
        }
        serviceRepository.deleteById(id);
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "services",allEntries = true),
        @CacheEvict(cacheNames = "serviceDetails",key = "#id")
    })
    @Transactional
    public void update(Long id, ServiceRequestDto serviceByRequest){
        Service serviceByBD = serviceRepository.findById(id).orElseThrow(()->new EntityNotFoundException("Объект не найден"));
        serviceByBD.setPrice(serviceByRequest.price());
        serviceByBD.setName(serviceByRequest.name());
    }

    @CacheEvict(allEntries = true)
    @Transactional
    public void add(ServiceRequestDto service, UserPrincipal userPrincipal){
        Stations stations = stationsRepository.
                findById(userPrincipal.stationId()).orElseThrow(()->new EntityNotFoundException("Станции с таким id не существует"));
        Service saveService = serviceMap.mapToService(service);
        saveService.setStation(stations);
        serviceRepository.save(saveService);
    }

    @Cacheable(cacheNames = "serviceDetails")
    public Service find(Long id) {
        return serviceRepository.findById(id).orElseThrow(()->new EntityNotFoundException("Объект не найден"));
    }

}