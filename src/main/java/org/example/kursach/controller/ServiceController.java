package org.example.kursach.controller;


import jakarta.validation.Valid;
import org.example.kursach.dto.ServiceRequestDto;
import org.example.kursach.entity.Service;
import org.example.kursach.service.ServiceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service")
public class ServiceController {
private ServiceService serviceService;

    public ServiceController(ServiceService serviceService) {
        this.serviceService = serviceService;
    }

    @GetMapping("/getAll")
    public List<Service> findAll(){
        return serviceService.findAll();
    }

    @GetMapping("/get/{id}")
    public Service find(@PathVariable Long id){
        return serviceService.find(id);
    }

    @DeleteMapping("/del/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id){
        serviceService.delete(id);
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public void add(@RequestBody @Valid ServiceRequestDto service){
        serviceService.add(service);
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','WORKER')")
    public void update(@PathVariable Long id,@RequestBody @Valid ServiceRequestDto service){
        serviceService.update(id,service);
    }
}
