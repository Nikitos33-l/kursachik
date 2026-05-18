package org.example.user.service.service;

import org.example.user.api.requestDto.CarRequestDto;
import org.example.user.api.requestDto.OrderVehicleMappingRequest;
import org.example.user.api.responceDto.VehicleDto;
import org.example.user.service.entity.Vehicle;
import org.example.user.service.mapper.VehicleMapper;
import org.example.user.service.repository.VehicleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CarServiceTest {
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private VehicleMapper vehicleMapper;

    @InjectMocks
    private CarService carService;

    private final UUID clientId = UUID.randomUUID();

    @Test
    @DisplayName("Сбор информации о машинах для списка заказов")
    void getVehiclesForOrders_Success() {
        Long orderId = 1L;
        Long vehicleId = 10L;
        OrderVehicleMappingRequest request = new OrderVehicleMappingRequest(orderId, vehicleId);

        Vehicle vehicle = Vehicle.builder().id(vehicleId).number("A111AA").build();
        VehicleDto dto = new VehicleDto(vehicleId, "Model", "Make", "A111AA");

        when(vehicleRepository.findAllByIdIn(Set.of(vehicleId))).thenReturn(List.of(vehicle));
        when(vehicleMapper.toDto(vehicle)).thenReturn(dto);

        Map<Long, VehicleDto> result = carService.getVehiclesForOrders(List.of(request));

        assertNotNull(result);
        assertTrue(result.containsKey(orderId));
        assertEquals(dto, result.get(orderId));

        // Меняем anySet() на any() для избежания конфликта типов дженериков
        verify(vehicleRepository).findAllByIdIn(any());
    }

    @Test
    @DisplayName("Создание мапы машин по ID")
    void getVehiclesMap_Success() {
        Set<Long> ids = Set.of(1L, 2L);
        Vehicle v1 = Vehicle.builder().id(1L).build();
        Vehicle v2 = Vehicle.builder().id(2L).build();

        when(vehicleRepository.findAllByIdIn(ids)).thenReturn(List.of(v1, v2));

        Map<Long, Vehicle> result = carService.getVehiclesMap(ids);

        assertEquals(2, result.size());
        assertEquals(v1, result.get(1L));
        assertEquals(v2, result.get(2L));
    }

    @Test
    @DisplayName("Получение существующей машины при вызове getOrCreateCar")
    void getOrCreateCar_ReturnsExisting() {
        // Передаем clientId (UUID) вместо 1L в конце
        CarRequestDto request = new CarRequestDto("Make","Model", "B222BB", clientId);
        Vehicle existingVehicle = Vehicle.builder().id(1L).number("B222BB").build();
        VehicleDto dto = new VehicleDto(1L, "Model", "Make", "B222BB");

        when(vehicleRepository.findByNumber(request.number())).thenReturn(Optional.of(existingVehicle));
        when(vehicleMapper.toDto(existingVehicle)).thenReturn(dto);

        VehicleDto result = carService.getOrCreateCar(request);

        assertNotNull(result);
        assertEquals("B222BB", result.number());
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Создание новой машины, если она не найдена по номеру")
    void getOrCreateCar_CreatesNew() {
        // Передаем clientId (UUID) вместо 1L в конце
        CarRequestDto request = new CarRequestDto("NewMake", "NewModel", "C333CC", clientId);
        VehicleDto dto = new VehicleDto(2L, "NewModel", "NewMake", "C333CC");

        when(vehicleRepository.findByNumber("C333CC")).thenReturn(Optional.empty());
        when(vehicleMapper.toDto(any(Vehicle.class))).thenReturn(dto);

        VehicleDto result = carService.getOrCreateCar(request);
        assertNotNull(result);
        assertEquals("C333CC", result.number());
        verify(vehicleRepository).save(argThat(v ->
                v.getNumber().equals("C333CC") && v.getModel().equals("NewModel")
        ));
    }
}