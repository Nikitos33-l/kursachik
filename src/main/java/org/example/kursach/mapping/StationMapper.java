package org.example.kursach.mapping;

import org.example.kursach.dto.AddressCoordinate;
import org.example.kursach.dto.RequestStationDto;
import org.example.kursach.entity.Stations;
import org.springframework.stereotype.Component;

@Component
public class StationMapper {

    public Stations toEntity(RequestStationDto dto, AddressCoordinate coords) {
        if (dto == null || coords == null) {
            return null;
        }
        Stations station = new Stations();
        station.setName(dto.name());
        station.setAddressText(dto.address());
        station.setLatitude(coords.latitude());
        station.setLongitude(coords.longitude());
        return station;
    }
}
