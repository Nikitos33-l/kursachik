package org.example.user.service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Импортируем логгер
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

@Slf4j
@Service
@RequiredArgsConstructor
public class CarService {
    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;

    @Transactional(readOnly = true)
    public Map<Long, VehicleDto> getVehiclesForOrders(List<OrderVehicleMappingRequest> request) {
        log.debug("Пакетный запрос автомобилей для сопоставления с {} заказами", request.size());

        Set<Long> vehiclesIds = request.stream()
                .map(OrderVehicleMappingRequest::vehicleId).collect(Collectors.toSet());

        Map<Long, Vehicle> vehicles = getVehiclesMap(vehiclesIds);

        return request.stream().collect(Collectors.toMap(
                OrderVehicleMappingRequest::orderId,
                r -> getVehicleOfOrder(r.vehicleId(), vehicles)
        ));
    }

    public Map<Long, Vehicle> getVehiclesMap(Set<Long> ids) {
        log.debug("Выборка автомобилей из БД по набору ID. Запрошено уникальных ID: {}", ids.size());
        return vehicleRepository.findAllByIdIn(ids).stream().collect(Collectors.toMap(
                Vehicle::getId, v -> v
        ));
    }

    public VehicleDto getVehicleOfOrder(Long vehicleId, Map<Long, Vehicle> vehicles) {
        return vehicleMapper.toDto(vehicles.get(vehicleId));
    }

    @Transactional
    public VehicleDto getOrCreateCar(CarRequestDto carRequest) {
        log.info("Запрос на получение или создание автомобиля. Госномер: '{}'", carRequest.number());

        Optional<Vehicle> optionalVehicle = vehicleRepository.findByNumber(carRequest.number());

        if (optionalVehicle.isPresent()) {
            log.info("Автомобиль с госномером '{}' найден в системе. Возвращаем существующую запись с ID: {}",
                    carRequest.number(), optionalVehicle.get().getId());
            return vehicleMapper.toDto(optionalVehicle.get());
        }

        log.info("Автомобиль с госномером '{}' не найден. Создаем новую запись в БД. Марка: {}, Модель: {}",
                carRequest.number(), carRequest.make(), carRequest.model());

        Vehicle vehicle = Vehicle.builder()
                .model(carRequest.model())
                .make(carRequest.make())
                .number(carRequest.number())
                .build();

        vehicleRepository.save(vehicle);
        log.info("Новый автомобиль успешно сохранен в БД. Сгенерирован ID: {}", vehicle.getId());

        return vehicleMapper.toDto(vehicle);
    }
}