package org.example.kursach.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.kursach.dto.RequestStationDto;
import org.example.kursach.service.StationsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
