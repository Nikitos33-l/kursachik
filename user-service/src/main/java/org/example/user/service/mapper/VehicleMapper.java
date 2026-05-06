package org.example.user.service.mapper;

import org.example.user.api.responceDto.VehicleDto;
import org.example.user.service.entity.Vehicle;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface VehicleMapper {

    VehicleDto toDto(Vehicle vehicle);

    Vehicle toEntity(VehicleDto vehicleDto);

    List<VehicleDto> toDtoList(List<Vehicle> vehicles);
}