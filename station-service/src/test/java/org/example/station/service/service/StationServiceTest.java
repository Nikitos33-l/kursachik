package org.example.station.service.service;

import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import org.example.order.service.api.common.client.OrderServiceClient;
import org.example.station.service.api.common.dto.request.RequestOrderMappingStationDto;
import org.example.station.service.api.common.dto.response.SummaryResponseStationDto;
import org.example.station.service.dto.AddressCoordinate;
import org.example.station.service.dto.request.RequestStationDto;
import org.example.station.service.dto.response.ResponseStationDto;
import org.example.station.service.entity.Station;
import org.example.station.service.mapper.StationMapper;
import org.example.station.service.repository.StationRepository;
import org.example.user.api.client.UserServiceFeignClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StationServiceTest {

    @Mock
    private GeocoderService geocoderService;
    @Mock
    private StationMapper stationMapper;
    @Mock
    private StationRepository stationRepository;
    @Mock
    private OrderServiceClient orderServiceClient;
    @Mock
    private UserServiceFeignClient userServiceClient;

    @InjectMocks
    private StationService stationService;

    @Test
    @DisplayName("Успешное добавление станции с геокодированием")
    void addStation_Success() {
        RequestStationDto request = new RequestStationDto("СТО 1", "Минск, Гикало 9");
        AddressCoordinate coords = new AddressCoordinate(new BigDecimal("53.9"), new BigDecimal("27.5"));
        Station station = new Station();

        when(geocoderService.getCoordinate(request.address())).thenReturn(coords);
        when(stationMapper.toEntity(request, coords)).thenReturn(station);

        stationService.addStation(request);

        verify(geocoderService).getCoordinate(request.address());
        verify(stationRepository).save(station);
    }

    @Test
    @DisplayName("Поиск всех станций")
    void findAll_Success() {
        when(stationRepository.findAll()).thenReturn(Collections.emptyList());
        when(stationMapper.toDtoList(any())).thenReturn(Collections.emptyList());

        List<ResponseStationDto> result = stationService.findAll();

        assertNotNull(result);
        verify(stationRepository).findAll();
    }

    @Test
    @DisplayName("Обновление станции: адрес изменился (вызов геокодера)")
    void update_AddressChanged_CallsGeocoder() {
        Long id = 1L;
        RequestStationDto dto = new RequestStationDto("Новое имя", "Новый адрес");
        Station existingStation = new Station();
        existingStation.setAddressText("Старый адрес");

        AddressCoordinate newCoords = new AddressCoordinate(new BigDecimal("10.0"), new BigDecimal("20.0"));

        when(stationRepository.findById(id)).thenReturn(Optional.of(existingStation));
        when(geocoderService.getCoordinate(dto.address())).thenReturn(newCoords);

        stationService.update(id, dto);

        assertEquals("Новый адрес", existingStation.getAddressText());
        assertEquals(new BigDecimal("20.0") , existingStation.getLatitude());
        verify(geocoderService).getCoordinate(anyString());
    }

    @Test
    @DisplayName("Обновление станции: адрес не изменился (геокодер не вызывается)")
    void update_AddressNotChanged_DoesNotCallGeocoder() {
        Long id = 1L;
        RequestStationDto dto = new RequestStationDto("Новое имя", "Тот же адрес");
        Station existingStation = new Station();
        existingStation.setAddressText("Тот же адрес");

        when(stationRepository.findById(id)).thenReturn(Optional.of(existingStation));

        stationService.update(id, dto);

        verify(geocoderService, never()).getCoordinate(anyString());
        assertEquals("Новое имя", existingStation.getName());
    }

    @Test
    @DisplayName("Удаление станции и связанных сущностей через Feign")
    void delete_Success() {
        Long id = 1L;

        stationService.delete(id);

        verify(stationRepository).deleteById(id);
        verify(orderServiceClient).deleteByStation(id);
        verify(userServiceClient).deleteWorkersByWorkplace(id);
    }

    @Test
    @DisplayName("Ошибка удаления при сбое Feign клиента")
    void delete_FeignException_ThrowsRuntimeException() {
        Long id = 1L;
        doThrow(FeignException.class).when(orderServiceClient).deleteByStation(id);

        assertThrows(RuntimeException.class, () -> stationService.delete(id));
    }

    @Test
    @DisplayName("Маппинг станций для списка заказов")
    void getStationsByOrders_Success() {
        Long orderId = 10L;
        Long stationId = 1L;
        RequestOrderMappingStationDto req = new RequestOrderMappingStationDto(orderId, stationId);

        Station station = new Station();
        station.setId(stationId);

        SummaryResponseStationDto summary = new SummaryResponseStationDto(stationId, "Name", "Address");

        when(stationRepository.findAllByIdIn(anySet())).thenReturn(List.of(station));
        when(stationMapper.toSummaryDto(station)).thenReturn(summary);

        Map<Long, SummaryResponseStationDto> result = stationService.getStationsByOrders(List.of(req));

        assertTrue(result.containsKey(orderId));
        assertEquals("Name", result.get(orderId).name());
    }

    @Test
    @DisplayName("Ошибка, если станция не найдена по ID")
    void getStationById_NotFound() {
        when(stationRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> stationService.findById(1L));
    }
}