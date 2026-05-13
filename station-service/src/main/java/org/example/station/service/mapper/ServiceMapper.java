package org.example.station.service.mapper;

import org.example.station.service.api.common.dto.response.ServiceDetailDto;
import org.example.station.service.dto.request.RequestServiceDto;
import org.example.station.service.dto.response.ResponseServiceDto;
import org.example.station.service.entity.Service;
import org.example.station.service.entity.Station;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ServiceMapper {

    ResponseServiceDto toDto(Service service);

    List<ResponseServiceDto> toDtoList(List<Service> services);

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "station", target = "station")
    @Mapping(source = "dto.name", target = "name")
    @Mapping(source = "dto.price", target = "price")
    Service toEntity(RequestServiceDto dto, Station station);

    List<ServiceDetailDto> toDtoDetailsList(List<Service> services);

}
