package org.example.kursach.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.kursach.dto.AddressCoordinate;
import org.example.kursach.dto.RequestStationDto;
import org.example.kursach.dto.ResponseStationDTO;
import org.example.kursach.entity.Stations;
import org.example.kursach.mapping.StationMapper;
import org.example.kursach.repository.StationsRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StationsService {
   private final StationsRepository stationsRepository;
   private final GeocoderService geocoderService;
   private final StationMapper stationMapper;

   public void addStations(RequestStationDto stationDto){
      AddressCoordinate addressCoordinate = geocoderService.getCoordinate(stationDto.address());
      Stations station = stationMapper.toEntity(stationDto,addressCoordinate);
      stationsRepository.save(station);
   }

    public ResponseStationDTO findById(Long id) {
        return stationsRepository.findById(id)
                .map(stationMapper::toDTO)
                .orElseThrow(() -> new EntityNotFoundException("Станция с id " + id + " не найдена"));
    }
}
