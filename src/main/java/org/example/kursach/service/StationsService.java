package org.example.kursach.service;

import lombok.RequiredArgsConstructor;
import org.example.kursach.repository.StationsRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StationsService {
   final StationsRepository stationsRepository;
   final GeocoderService geocoderService;

   public void addStations(){

   }

}
