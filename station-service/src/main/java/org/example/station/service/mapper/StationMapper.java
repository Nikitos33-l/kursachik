package org.example.station.service.mapper;

import org.example.station.service.api.common.dto.response.SummaryResponseStationDto;
import org.example.station.service.dto.AddressCoordinate;
import org.example.station.service.dto.request.RequestStationDto;
import org.example.station.service.dto.response.ResponseStationDto;
import org.example.station.service.entity.Station;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface StationMapper {
    @Mapping(source = "dto.name", target = "name")
    @Mapping(source = "dto.address", target = "addressText")
    @Mapping(source = "coordinate.latitude", target = "latitude")
    @Mapping(source = "coordinate.longitude", target = "longitude")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderIds", ignore = true)
    @Mapping(target = "services", ignore = true)
    Station toEntity(RequestStationDto dto, AddressCoordinate coordinate);

    @Mapping(source = "addressText", target = "address")
    ResponseStationDto toDto(Station station);

    List<ResponseStationDto> toDtoList(List<Station> stations);

    @Mapping(source = "id", target = "stationId")
    @Mapping(source = "addressText", target = "address")
    SummaryResponseStationDto toSummaryDto(Station station);
}
