package org.example.station.service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.securitycommon.UserPrincipal;
import org.example.station.service.api.common.dto.response.StationServicesResponse;
import org.example.station.service.dto.request.RequestServiceDto;
import org.example.station.service.dto.response.ResponseServiceDto;
import org.example.station.service.service.ServiceManagementService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service")
@RequiredArgsConstructor
public class ServiceController {
    private final ServiceManagementService service;

    @GetMapping({"/getAll","/getAll/{stationId}"})
    public List<ResponseServiceDto> getAll
            (@PathVariable(required = false) Long stationId,
             @AuthenticationPrincipal UserPrincipal userPrincipal)
    {
        return service.findAll(stationId,userPrincipal);
    }

    @GetMapping("/get/{id}")
    public ResponseServiceDto getById(@PathVariable Long id){
        return service.findById(id);
    }

    @DeleteMapping("/del/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id){
        service.delete(id);
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public void add(@RequestBody @Valid RequestServiceDto serviceDto, @AuthenticationPrincipal UserPrincipal userPrincipal){
        service.add(serviceDto,userPrincipal);
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','WORKER')")
    public void update(@PathVariable Long id,@RequestBody @Valid RequestServiceDto serviceDto){
        service.update(id,serviceDto);
    }

    @GetMapping("/internal/{stationId}/validate")
    StationServicesResponse validateStationAndGetServices(
            @PathVariable Long stationId,
            @RequestBody List<Long> serviceIds
    ){
        return service.findByIdsAndValidateService(serviceIds,stationId);
    }

}
