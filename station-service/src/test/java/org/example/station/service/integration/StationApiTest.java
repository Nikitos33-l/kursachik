package org.example.station.service.integration;

import org.awaitility.Awaitility;
import org.example.station.service.api.common.dto.request.RequestOrderMappingStationDto;
import org.example.station.service.dto.AddressCoordinate;
import org.example.station.service.dto.request.RequestStationDto;
import org.example.station.service.entity.Station;
import org.example.station.service.service.GeocoderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StationApiTest extends BaseIntegrationTests {

    @MockitoBean
    private GeocoderService geocoderService;

    @MockitoSpyBean
    private RabbitTemplate rabbitTemplate;

    @Value("${station.exchange.name}")
    private String stationExchange;

    @Value("${station.delete.routing.key}")
    private String stationDeletedRoutingKey;

    private final UUID authUserId = UUID.randomUUID();
    private final BigDecimal defaultLon = new BigDecimal("27.566700");
    private final BigDecimal defaultLat = new BigDecimal("53.900000");

    @Test
    @DisplayName("POST /api/stations/add: Успешное добавление станции супер-админом")
    void shouldAddStationSuccessfullyBySuperAdmin() throws Exception {
        RequestStationDto requestDto = new RequestStationDto("Станция Восток", "Минск, Независимости 116");
        AddressCoordinate mockCoordinate = new AddressCoordinate(defaultLon, defaultLat);

        when(geocoderService.getCoordinate(requestDto.address())).thenReturn(mockCoordinate);

        mockMvc.perform(post("/api/stations/add")
                        .headers(getSecurityHeaders("ROLE_SUPERADMIN", null, authUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk());

        List<Station> stations = stationRepository.findAll();
        assertThat(stations).hasSize(1);
        assertThat(stations.get(0).getName()).isEqualTo("Станция Восток");
        assertThat(stations.get(0).getAddressText()).isEqualTo("Минск, Независимости 116");
    }

    @Test
    @DisplayName("POST /api/stations/add: Отказ в доступе, если роль не SUPERADMIN")
    void shouldReturnForbiddenWhenAddStationNotSuperAdmin() throws Exception {
        RequestStationDto requestDto = new RequestStationDto("Станция Тест", "Минск, Ленина 1");

        mockMvc.perform(post("/api/stations/add")
                        .headers(getSecurityHeaders("ROLE_ADMIN", 1L, authUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isForbidden());

        assertThat(stationRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("GET /api/stations/findById/{id}: Успешное получение станции по ID")
    void shouldGetStationById() throws Exception {
        Station savedStation = createAndSaveStation("Центральная", "Минск, Берута 3");

        mockMvc.perform(get("/api/stations/findById/{id}", savedStation.getId())
                        .headers(getSecurityHeaders("ROLE_USER", savedStation.getId(), authUserId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedStation.getId()))
                .andExpect(jsonPath("$.name").value("Центральная"))
                .andExpect(jsonPath("$.address").value("Минск, Берута 3"));
    }

    @Test
    @DisplayName("GET /api/stations/findAll: Успешное получение списка всех станций")
    void shouldGetAllStations() throws Exception {
        createAndSaveStation("Станция 1", "Адрес 1");
        createAndSaveStation("Станция 2", "Адрес 2");

        mockMvc.perform(get("/api/stations/findAll")
                        .headers(getSecurityHeaders("ROLE_USER", null, authUserId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Станция 1"))
                .andExpect(jsonPath("$[1].name").value("Станция 2"));
    }

    @Test
    @DisplayName("PUT /api/stations/update/{id}: Успешное обновление имени и адреса с вызовом геокодера")
    void shouldUpdateStationWithAddressChange() throws Exception {
        Station station = createAndSaveStation("Старое имя", "Старый адрес");
        RequestStationDto updateDto = new RequestStationDto("Новое имя", "Новый адрес");

        BigDecimal newLon = new BigDecimal("28.100000");
        BigDecimal newLat = new BigDecimal("54.100000");
        when(geocoderService.getCoordinate(updateDto.address())).thenReturn(new AddressCoordinate(newLon, newLat));

        mockMvc.perform(put("/api/stations/update/{id}", station.getId())
                        .headers(getSecurityHeaders("ROLE_SUPERADMIN", null, authUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk());

        Station updatedStation = stationRepository.findById(station.getId()).orElseThrow();
        assertThat(updatedStation.getName()).isEqualTo("Новое имя");
        assertThat(updatedStation.getAddressText()).isEqualTo("Новый адрес");
        verify(geocoderService, times(1)).getCoordinate("Новый адрес");
    }

    @Test
    @DisplayName("PUT /api/stations/update/{id}: Обновление только имени (адрес совпадает, геокодер не вызывается)")
    void shouldUpdateStationWithoutAddressChange() throws Exception {
        Station station = createAndSaveStation("Старое имя", "Минск, Ленина 1");
        RequestStationDto updateDto = new RequestStationDto("Новое имя", "минск, ленина 1");

        mockMvc.perform(put("/api/stations/update/{id}", station.getId())
                        .headers(getSecurityHeaders("ROLE_SUPERADMIN", null, authUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk());

        Station updatedStation = stationRepository.findById(station.getId()).orElseThrow();
        assertThat(updatedStation.getName()).isEqualTo("Новое имя");
        verify(geocoderService, never()).getCoordinate(anyString());
    }

    @Test
    @DisplayName("DELETE /api/stations/delete/{id}: Полная проверка удаления и асинхронной отправки сообщения в RabbitMQ")
    void shouldDeleteStationAndVerifyRabbitMessage() throws Exception {
        Station station = createAndSaveStation("Удаляемая станция", "Адрес");
        Long deletedId = station.getId();

        mockMvc.perform(delete("/api/stations/delete/{id}", deletedId)
                        .headers(getSecurityHeaders("ROLE_SUPERADMIN", null, authUserId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        assertThat(stationRepository.findById(deletedId)).isEmpty();

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(rabbitTemplate).send(
                        eq(stationExchange), eq(stationDeletedRoutingKey), any(Message.class)
                ));
    }

    @Test
    @DisplayName("GET /api/stations/internal/getAll/by/station: Успешное маппирование станций по списку заказов")
    void shouldGetStationsByOrdersInternal() throws Exception {
        Station station = createAndSaveStation("Заказная Станция", "Улица Тестов");

        List<RequestOrderMappingStationDto> requestList = List.of(
                new RequestOrderMappingStationDto(555L, station.getId())
        );

        mockMvc.perform(post("/api/stations/internal/getAll/by/station")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestList)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['555']").exists())
                .andExpect(jsonPath("$['555'].name").value("Заказная Станция"));
    }

    private Station createAndSaveStation(String name, String address) {
        Station station = Station.builder()
                .name(name)
                .addressText(address)
                .longitude(defaultLon)
                .latitude(defaultLat)
                .build();
        return stationRepository.save(station);
    }
}