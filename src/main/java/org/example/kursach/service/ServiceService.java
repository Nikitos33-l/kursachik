package org.example.kursach.service;


import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.example.kursach.dto.ServiceRequestDto;
import org.example.kursach.entity.Service;
import org.example.kursach.mapping.Service_map;
import org.example.kursach.repository.ServiceRepository;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;


import java.util.List;

@org.springframework.stereotype.Service
@CacheConfig(cacheNames = {"services"})
public class ServiceService {
    private final ServiceRepository serviceRepository;
    private final Service_map service_map;

    public ServiceService(ServiceRepository serviceRepository, Service_map serviceMap){
        this.serviceRepository=serviceRepository;
        service_map = serviceMap;
    }

    @Cacheable
    public List<Service> findAll(){
        return serviceRepository.findAll();
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(key = "T(org.springframework.cache.interceptor.SimpleKey).EMPTY"),
            @CacheEvict(key = "#id")
    })
    public void delete(Long id){
        if (!serviceRepository.existsById(id)){
             throw new EntityNotFoundException("Объект не найден");
        }
        serviceRepository.deleteById(id);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(key = "T(org.springframework.cache.interceptor.SimpleKey).EMPTY"),
            @CacheEvict(key = "#id")
    })
    public void update(Long id, ServiceRequestDto service_by_request){
        Service service_by_BD = serviceRepository.findById(id).orElseThrow(()->new EntityNotFoundException("Объект не найден"));
        service_by_BD.setPrice(service_by_request.price());
        service_by_BD.setName(service_by_request.name());
    }

    @Transactional
    @CacheEvict(key = "T(org.springframework.cache.interceptor.SimpleKey).EMPTY")
    public void add(ServiceRequestDto service){
        serviceRepository.save(service_map.map_to_service(service));
    }

    @Cacheable
    public Service find(Long id) {
        return serviceRepository.findById(id).orElseThrow(()->new EntityNotFoundException("Объект не найден"));
    }

}
