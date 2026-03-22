package org.example.kursach.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.kursach.dto.RequestStationDto;
import org.example.kursach.dto.ResponseStationDTO;
import org.example.kursach.service.StationsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/stations")
@RequiredArgsConstructor
public class StationsController {
    private final StationsService stationsService;

    @PreAuthorize("hasRole(SUPERADMIN)")
    @PostMapping("/add")
    public void addStation(@RequestBody @Valid RequestStationDto stationDto){
        stationsService.addStations(stationDto);
    }

    @GetMapping("/findById/{id}")
    public ResponseStationDTO getStation(@PathVariable Long id){
       return stationsService.findById(id);
    }
}
