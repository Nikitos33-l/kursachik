package org.example.kursach;

import org.example.kursach.dto.AddressCoordinate;
import org.example.kursach.dto.RequestStationDto;
import org.example.kursach.entity.Stations;
import org.example.kursach.repository.StationsRepository;
import org.example.kursach.service.GeocoderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class StationsIntegrationsTest extends KursachApplicationTests {

    @Autowired
    private StationsRepository stationsRepository;

    @MockitoBean
    private GeocoderService geocoderService;

    @BeforeEach
    public void cleanDB(){
        stationsRepository.deleteAll();
    }



    @Test
    @DisplayName("Успешное создание новой станции")
    void shouldCreateNewStationSuccessfully() throws Exception {
        RequestStationDto dto = new RequestStationDto("СТО 'У Антона'", "Минск, Гикало 9");
        AddressCoordinate mockCoords = new AddressCoordinate(
                new BigDecimal("27.5925"),
                new BigDecimal("53.9163")
        );

        when(geocoderService.getCoordinate(anyString())).thenReturn(mockCoords);

        mockMvc.perform(post("/api/stations/add")
                        .with(user("admin").roles("SUPERADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        List<Stations> allStations = stationsRepository.findAll();

        assertThat(allStations).hasSize(1);
        Stations savedStation = allStations.get(0);

        assertThat(savedStation.getName()).isEqualTo("СТО 'У Антона'");
        assertThat(savedStation.getAddressText()).isEqualTo("Минск, Гикало 9");
        assertThat(savedStation.getLatitude()).isEqualByComparingTo("53.9163");
        assertThat(savedStation.getLongitude()).isEqualByComparingTo("27.5925");
    }

    @Test
    @DisplayName("Успешное обновление данных станции")
    void shouldUpdateStationSuccessfully() throws Exception {
        Stations savedStation = saveTestStation("Старое название", "Старый адрес");

        RequestStationDto updateDto = new RequestStationDto("СТО 'Обновленное'", "Минск, Победителей 1");
        AddressCoordinate newMockCoords = new AddressCoordinate(
                new BigDecimal("27.5500"),
                new BigDecimal("53.9000")
        );

        when(geocoderService.getCoordinate("Минск, Победителей 1")).thenReturn(newMockCoords);

        mockMvc.perform(put("/api/stations/update/" + savedStation.getId())
                        .with(user("admin").roles("SUPERADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk());

        Stations updatedStation = stationsRepository.findById(savedStation.getId()).orElseThrow();

        assertThat(updatedStation.getName()).isEqualTo("СТО 'Обновленное'");
        assertThat(updatedStation.getAddressText()).isEqualTo("Минск, Победителей 1");
        assertThat(updatedStation.getLatitude()).isEqualByComparingTo("53.9000");
        assertThat(updatedStation.getLongitude()).isEqualByComparingTo("27.5500");
    }

    @Test
    @DisplayName("Успешное удаление станции")
    void shouldDeleteStationSuccessfully() throws Exception {
        Stations savedStation = saveTestStation("СТО под снос", "Адрес под снос");

        assertThat(stationsRepository.findAll()).hasSize(1);

        mockMvc.perform(delete("/api/stations/delete/" + savedStation.getId())
                        .with(user("admin").roles("SUPERADMIN")))
                .andExpect(status().isOk());

        List<Stations> allStations = stationsRepository.findAll();
        assertThat(allStations).isEmpty();
    }

    private Stations saveTestStation(String name, String address) {
        Stations station = new Stations();
        station.setName(name);
        station.setAddressText(address);
        station.setLatitude(new BigDecimal("53.9000"));
        station.setLongitude(new BigDecimal("27.5500"));
        return stationsRepository.save(station);
    }


}

