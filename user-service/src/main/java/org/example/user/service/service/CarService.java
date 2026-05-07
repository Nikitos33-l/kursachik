package org.example.user.service.service;

import lombok.RequiredArgsConstructor;
import org.example.user.api.requestDto.CarRequestDto;
import org.example.user.api.requestDto.OrderVehicleMappingRequest;
import org.example.user.api.responceDto.VehicleDto;
import org.example.user.service.entity.Vehicle;
import org.example.user.service.mapper.VehicleMapper;
import org.example.user.service.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CarService {
    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;

    @Transactional(readOnly = true)
    public Map<Long, VehicleDto> getVehiclesForOrders(List<OrderVehicleMappingRequest> request) {
        Set<Long> vehiclesIds = request.stream()
                .map(OrderVehicleMappingRequest::vehicleId).collect(Collectors.toSet());

        Map<Long, Vehicle> vehicles = getVehiclesMap(vehiclesIds);

        return request.stream().collect(Collectors.toMap(
                OrderVehicleMappingRequest::orderId,
                r-> getVehicleOfOrder(r.vehicleId(),vehicles)
        ));
    }

    public Map<Long,Vehicle> getVehiclesMap(Set<Long> ids){
        return vehicleRepository.findAllByIdIn(ids).stream().collect(Collectors.toMap(
                Vehicle::getId,v->v
        ));
    }
    public VehicleDto getVehicleOfOrder(Long vehicleId,Map<Long,Vehicle> vehicles){
        return vehicleMapper.toDto(vehicles.get(vehicleId));
    }

    @Transactional
    public VehicleDto getOrCreateCar(CarRequestDto carRequest) {
        Optional<Vehicle> optionalVehicle = vehicleRepository.findByNumber(carRequest.number());
        if(optionalVehicle.isPresent()){
            return vehicleMapper.toDto(optionalVehicle.get());
        }
        Vehicle vehicle = Vehicle.builder().model(carRequest.model())
                .make(carRequest.make()).number(carRequest.number()).build();

        vehicleRepository.save(vehicle);
        return vehicleMapper.toDto(vehicle);
    }
}
