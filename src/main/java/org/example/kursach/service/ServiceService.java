package org.example.kursach.service;


import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.example.kursach.dto.ServiceRequestDto;
import org.example.kursach.dto.UserPrincipal;
import org.example.kursach.entity.Service;
import org.example.kursach.entity.Stations;
import org.example.kursach.entity.User;
import org.example.kursach.mapping.ServiceMap;
import org.example.kursach.repository.ServiceRepository;
import org.example.kursach.repository.UserRepository;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;


import java.util.List;

@org.springframework.stereotype.Service
@CacheConfig(cacheNames = {"services"})
public class ServiceService {
    private final ServiceRepository serviceRepository;
    private final ServiceMap service_map;
    private final UserRepository userRepository;

    public ServiceService(ServiceRepository serviceRepository, ServiceMap serviceMap, UserRepository userRepository){
        this.serviceRepository=serviceRepository;
        service_map = serviceMap;
        this.userRepository = userRepository;
    }

    @Cacheable(key = "#stationId")
    public List<Service> findAll(Long stationId){
        return serviceRepository.findByStationId(stationId);
    }

    @CacheEvict(allEntries = true)
    @Transactional
    public void delete(Long id){
        if (!serviceRepository.existsById(id)){
             throw new EntityNotFoundException("Объект не найден");
        }
        serviceRepository.deleteById(id);
    }

    @CacheEvict(allEntries = true)
    @Transactional
    public void update(Long id, ServiceRequestDto service_by_request){
        Service service_by_BD = serviceRepository.findById(id).orElseThrow(()->new EntityNotFoundException("Объект не найден"));
        service_by_BD.setPrice(service_by_request.price());
        service_by_BD.setName(service_by_request.name());
    }

    @CacheEvict(allEntries = true)
    @Transactional
    public void add(ServiceRequestDto service, UserPrincipal userPrincipal){
        String adminEmail = userPrincipal.email();
        User adminOfStation = userRepository.findByEmail(adminEmail);
        Stations stations = adminOfStation.getWorkplace();
        Service saveService = service_map.mapToService(service);
        saveService.setStation(stations);
        serviceRepository.save(saveService);
    }

    @Cacheable(cacheNames = "serviceDetails")
    public Service find(Long id) {
        return serviceRepository.findById(id).orElseThrow(()->new EntityNotFoundException("Объект не найден"));
    }

}