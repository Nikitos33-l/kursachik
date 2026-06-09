package org.example.station.service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.station.service.api.common.dto.request.RequestOrderMappingStationDto;
import org.example.station.service.api.common.dto.response.SummaryResponseStationDto;
import org.example.station.service.dto.request.RequestStationDto;
import org.example.station.service.dto.response.ResponseStationDto;
import org.example.station.service.service.StationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stations")
@RequiredArgsConstructor
public class StationController {
    private final StationService stationService;

    @PreAuthorize("hasRole('SUPERADMIN')")
    @PostMapping("/add")
    public void addStation(@RequestBody @Valid RequestStationDto requestStationDto){
        stationService.addStation(requestStationDto);
    }

    @GetMapping("/findById/{id}")
    public ResponseStationDto getStation(@PathVariable Long id){
        return stationService.findById(id);
    }

    @GetMapping("/findAll")
    public List<ResponseStationDto> findAll(){
        return stationService.findAll();
    }

    @PreAuthorize("hasRole('SUPERADMIN')")
    @DeleteMapping("/delete/{id}")
    public void deleteStation(@PathVariable Long id){
        stationService.delete(id);
    }

    @PreAuthorize("hasRole('SUPERADMIN')")
    @PutMapping("/update/{id}")
    public void updateStation(@PathVariable Long id,@RequestBody @Valid RequestStationDto stationDto){
        stationService.update(id,stationDto);
    }

    @PostMapping("/internal/getAll/by/station")
    public Map<Long, SummaryResponseStationDto> getStationsByOrders(@RequestBody List<RequestOrderMappingStationDto> request){
        return stationService.getStationsByOrders(request);
    }

}