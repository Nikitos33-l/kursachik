package org.example.kursach.controller;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.example.kursach.dto.ServiceRequestDto;
import org.example.kursach.dto.UserPrincipal;
import org.example.kursach.entity.Service;
import org.example.kursach.service.JWTService;
import org.example.kursach.service.ServiceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service")
public class ServiceController {
    private final ServiceService service;
    private final JWTService jwtService;

    public ServiceController(ServiceService serviceService, JWTService jwtService) {
        this.service = serviceService;
        this.jwtService = jwtService;
    }

    @GetMapping("/getAll")
    public List<Service> findAll(){
        return service.findAll();
    }

    @GetMapping("/get/{id}")
    public Service find(@PathVariable Long id){
        return service.find(id);
    }

    @DeleteMapping("/del/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id){
        service.delete(id);
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public void add(@RequestBody @Valid ServiceRequestDto service, @AuthenticationPrincipal UserPrincipal userPrincipal){
        this.service.add(service,userPrincipal);
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','WORKER')")
    public void update(@PathVariable Long id,@RequestBody @Valid ServiceRequestDto service){
        this.service.update(id,service);
    }
}
