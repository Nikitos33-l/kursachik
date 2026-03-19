package org.example.kursach.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.kursach.dto.RequestStationDto;
import org.example.kursach.service.StationsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/stations")
@RequiredArgsConstructor
public class StationsController {
    private final StationsService stationsService;

    @PostMapping("/add")
    public void addStation(@Valid RequestStationDto stationDto){
        stationsService.addStations(stationDto);
    }
}
