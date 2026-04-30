package org.example.kursach.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.kursach.dto.AddressCoordinate;
import org.example.kursach.dto.RequestStationDto;
import org.example.kursach.dto.ResponseStationDTO;
import org.example.kursach.entity.Stations;
import org.example.kursach.mapping.StationMapper;
import org.example.kursach.repository.StationsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StationServiceTest {
    @Mock
    private StationsRepository stationsRepository;

    @Mock
    private GeocoderService geocoderService;

    @Mock
    private StationMapper stationMapper;

    @InjectMocks
    private StationsService stationsService;

    @Test
    @DisplayName("Добавление станции: успех")
    void addStationsSuccess() {
        RequestStationDto dto = new RequestStationDto("СТО Тест", "Минск, Гикало 9");
        AddressCoordinate coords = new AddressCoordinate(new BigDecimal("27.5"), new BigDecimal("53.9"));
        Stations stationEntity = new Stations();

        when(geocoderService.getCoordinate(dto.address())).thenReturn(coords);
        when(stationMapper.toEntity(dto, coords)).thenReturn(stationEntity);

        stationsService.addStations(dto);

        verify(geocoderService).getCoordinate(dto.address());
        verify(stationMapper).toEntity(dto, coords);
        verify(stationsRepository).save(stationEntity);
    }

    @Test
    @DisplayName("Поиск по ID: станция найдена")
    void findById_Found() {
        Long id = 1L;
        Stations station = new Stations();
        ResponseStationDTO expectedDto = new ResponseStationDTO(1L,new BigDecimal("27.5"),new BigDecimal("53.9"),"СТО", "Минск, Гикало 9");

        when(stationsRepository.findById(id)).thenReturn(Optional.of(station));
        when(stationMapper.toDTO(station)).thenReturn(expectedDto);

        ResponseStationDTO result = stationsService.findById(id);

        assertThat(result).isEqualTo(expectedDto);
        verify(stationsRepository).findById(id);
    }

    @Test
    @DisplayName("Поиск по ID: станция НЕ найдена (Exception)")
    void findById_NotFound_ThrowsException() {
        Long id = 999L;
        when(stationsRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stationsService.findById(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Станция с id " + id + " не найдена");

        verifyNoInteractions(stationMapper);
    }
}

