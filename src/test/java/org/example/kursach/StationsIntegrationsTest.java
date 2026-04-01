package org.example.kursach;

import org.example.kursach.dto.AddressCoordinate;
import org.example.kursach.dto.RequestStationDto;
import org.example.kursach.entity.Stations;
import org.example.kursach.repository.StationsRepository;
import org.example.kursach.service.GeocoderService;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    void shouldCreateNewStationSuccessfully() throws Exception {
        RequestStationDto dto = new RequestStationDto("СТО 'У Антона'", "Минск, Гикало 9");
        AddressCoordinate mockCoords = new AddressCoordinate(
                new BigDecimal("27.5925"),
                new BigDecimal("53.9163")
        );

        when(geocoderService.getCoordinate(anyString())).thenReturn(mockCoords);

        mockMvc.perform(post("/api/stations/add")
                        .with(user("admin").roles("ADMIN"))
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


}

