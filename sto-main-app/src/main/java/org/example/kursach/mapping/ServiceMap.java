package org.example.kursach.mapping;

import org.example.kursach.dto.ServiceRequestDto;
import org.example.kursach.entity.Service;
import org.springframework.stereotype.Component;

@Component
public class ServiceMap {
    public Service mapToService(ServiceRequestDto serviceRequestDto){
        Service service = new Service();
        service.setName(serviceRequestDto.name());
        service.setPrice(serviceRequestDto.price());
        return  service;
    }
}
