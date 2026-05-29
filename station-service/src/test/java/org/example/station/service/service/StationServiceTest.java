package org.example.station.service.service;

import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import org.example.station.service.api.common.dto.request.RequestOrderMappingStationDto;
import org.example.station.service.api.common.dto.response.SummaryResponseStationDto;
import org.example.station.service.dto.AddressCoordinate;
import org.example.station.service.dto.request.RequestStationDto;
import org.example.station.service.dto.response.ResponseStationDto;
import org.example.station.service.entity.Station;
import org.example.station.service.mapper.StationMapper;
import org.example.station.service.producer.StationEventProducer;
import org.example.station.service.repository.StationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StationServiceTest {

    @Mock private GeocoderService geocoderService;
    @Mock private StationMapper stationMapper;
    @Mock private StationRepository stationRepository;
    @Mock private StationEventProducer stationEventProducer;

    @InjectMocks
    private StationService stationService;

    private Station sampleStation;
    private final Long stationId = 1L;

    private final BigDecimal defaultLat = new BigDecimal("53.900000");
    private final BigDecimal defaultLon = new BigDecimal("27.566700");

    @BeforeEach
    void setUp() {
        sampleStation = new Station();
        sampleStation.setId(stationId);
        sampleStation.setName("Main Station");
        sampleStation.setAddressText("Minsk, Lenina 1");
        sampleStation.setLatitude(defaultLat);
        sampleStation.setLongitude(defaultLon);
    }

    @Test
    @DisplayName("Успешное добавление станции")
    void addStation_Success() {
        RequestStationDto requestDto = new RequestStationDto("New Station", "Minsk, Pobediteley 5");
        AddressCoordinate coordinate = new AddressCoordinate(defaultLon, defaultLat);

        when(geocoderService.getCoordinate(requestDto.address())).thenReturn(coordinate);
        when(stationMapper.toEntity(requestDto, coordinate)).thenReturn(sampleStation);

        stationService.addStation(requestDto);

        verify(geocoderService, times(1)).getCoordinate(requestDto.address());
        verify(stationRepository, times(1)).save(sampleStation);
    }

    @Test
    @DisplayName("Успешное получение списка всех станций")
    void findAll_Success() {
        // В ResponseStationDto порядок: id, longitude, latitude, name, address
        ResponseStationDto responseDto = new ResponseStationDto(
                stationId, defaultLon, defaultLat, "Main Station", "Minsk, Lenina 1"
        );

        when(stationRepository.findAll()).thenReturn(List.of(sampleStation));
        when(stationMapper.toDtoList(anyList())).thenReturn(List.of(responseDto));

        List<ResponseStationDto> result = stationService.findAll();

        assertEquals(1, result.size());
        assertEquals("Main Station", result.get(0).name());
        verify(stationRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Успешное получение станции по ID")
    void findById_Success() {
        ResponseStationDto responseDto = new ResponseStationDto(
                stationId, defaultLon, defaultLat, "Main Station", "Minsk, Lenina 1"
        );

        when(stationRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));
        when(stationMapper.toDto(sampleStation)).thenReturn(responseDto);

        ResponseStationDto result = stationService.findById(stationId);

        assertNotNull(result);
        assertEquals(stationId, result.id());
    }

    @Test
    @DisplayName("Ошибка при получении станции по несуществующему ID")
    void findById_NotFound_ThrowsException() {
        when(stationRepository.findById(stationId)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> stationService.findById(stationId));

        assertEquals("Станция с таким id не была найдена", exception.getMessage());
    }

    @Test
    @DisplayName("Успешное обновление станции с изменением адреса (вызов геокодера)")
    void update_WithAddressChange_Success() {
        RequestStationDto updateDto = new RequestStationDto("Updated Name", "New Address 123");

        BigDecimal newLat = new BigDecimal("10.000000");
        BigDecimal newLon = new BigDecimal("20.000000");
        AddressCoordinate newCoordinate = new AddressCoordinate(newLon, newLat);

        when(stationRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));
        when(geocoderService.getCoordinate(updateDto.address())).thenReturn(newCoordinate);

        stationService.update(stationId, updateDto);

        assertEquals("Updated Name", sampleStation.getName());
        assertEquals("New Address 123", sampleStation.getAddressText());
        assertEquals(newLat, sampleStation.getLatitude());
        assertEquals(newLon, sampleStation.getLongitude());
        verify(geocoderService, times(1)).getCoordinate(anyString());
    }

    @Test
    @DisplayName("Успешное обновление станции без изменения адреса (геокодер не вызывается)")
    void update_WithoutAddressChange_Success() {
        RequestStationDto updateDto = new RequestStationDto("Updated Name", "minsk, lenina 1");

        when(stationRepository.findById(stationId)).thenReturn(Optional.of(sampleStation));

        stationService.update(stationId, updateDto);

        assertEquals("Updated Name", sampleStation.getName());
        verify(geocoderService, never()).getCoordinate(anyString());
        assertEquals(defaultLat, sampleStation.getLatitude());
    }

    @Test
    @DisplayName("Успешное удаление станции и отправка события в RabbitMQ")
    void delete_Success() {
        try (MockedStatic<TransactionSynchronizationManager> tsmMock = mockStatic(TransactionSynchronizationManager.class)) {
            tsmMock.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);

            tsmMock.when(() -> TransactionSynchronizationManager.registerSynchronization(any(TransactionSynchronization.class)))
                    .thenAnswer(invocation -> {
                        TransactionSynchronization sync = invocation.getArgument(0);
                        sync.afterCommit();
                        return null;
                    });

            stationService.delete(stationId);

            verify(stationRepository, times(1)).deleteById(stationId);
            verify(stationEventProducer, times(1)).publishUserDeletedEvent(stationId);
        }
    }

    @Test
    @DisplayName("Ошибка при удалении: перехват FeignException")
    void delete_ThrowsFeignException() {
        FeignException mockFeignException = mock(FeignException.class);
        doThrow(mockFeignException).when(stationRepository).deleteById(stationId);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> stationService.delete(stationId));

        assertEquals("Не удалось удалить связанные сущности", exception.getMessage());
        verify(stationEventProducer, never()).publishUserDeletedEvent(any());
    }

    @Test
    @DisplayName("Успешное сопоставление станций с заказами (getStationsByOrders)")
    void getStationsByOrders_Success() {
        Long orderId = 100L;
        RequestOrderMappingStationDto request = new RequestOrderMappingStationDto(orderId, stationId);

        SummaryResponseStationDto summaryDto = mock(SummaryResponseStationDto.class);

        when(stationRepository.findAllByIdIn(Set.of(stationId))).thenReturn(List.of(sampleStation));
        when(stationMapper.toSummaryDto(sampleStation)).thenReturn(summaryDto);

        Map<Long, SummaryResponseStationDto> result = stationService.getStationsByOrders(List.of(request));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey(orderId));
        assertEquals(summaryDto, result.get(orderId));
    }
}