package org.example.station.service.integration;

import org.awaitility.Awaitility;
import org.example.station.service.dto.request.RequestServiceDto;
import org.example.station.service.entity.Service;
import org.example.station.service.entity.Station;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ServiceApiTest extends BaseIntegrationTests {

    @MockitoSpyBean
    private RabbitTemplate rabbitTemplate;

    @Value("${station.exchange.name:station-exchange}")
    private String stationExchange;

    @Value("${station.services.updated.routing.key:services-updated-key}")
    private String servicesUpdatedRoutingKey;

    private final UUID authUserId = UUID.randomUUID();

    @Test
    @DisplayName("POST /api/service/add: Успешное добавление услуги админом")
    void shouldAddServiceSuccessfully() throws Exception {
        Station station = createAndSaveStation("Станция ТехОбслуживания");
        RequestServiceDto requestDto = new RequestServiceDto("Замена масла", new BigDecimal("150.00"));

        mockMvc.perform(post("/api/service/add")
                        .headers(getSecurityHeaders("ROLE_ADMIN", station.getId(), authUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk());

        List<Service> services = serviceRepository.findAll();
        assertThat(services).hasSize(1);
        assertThat(services.get(0).getName()).isEqualTo("Замена масла");
        assertThat(services.get(0).getStation().getId()).isEqualTo(station.getId());
    }

    @Test
    @DisplayName("POST /api/service/add: Ошибка 403, если у пользователя нет роли ADMIN")
    void shouldFailToAddServiceIfRoleIsInvalid() throws Exception {
        Station station = createAndSaveStation("Станция Тест");
        RequestServiceDto requestDto = new RequestServiceDto("Диагностика", new BigDecimal("50.00"));

        mockMvc.perform(post("/api/service/add")
                        .headers(getSecurityHeaders("ROLE_USER", station.getId(), authUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isForbidden());

        assertThat(serviceRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("PUT /api/service/update/{id}: Обновление услуги воркером и асинхронный триггер RabbitMQ")
    void shouldUpdateServiceAndSendRabbitMessage() throws Exception {
        Station station = createAndSaveStation("Станция 1");
        Service service = createAndSaveService("Старая услуга", new BigDecimal("100.00"), station);

        RequestServiceDto updateDto = new RequestServiceDto("Новая услуга", new BigDecimal("200.00"));

        mockMvc.perform(put("/api/service/update/{id}", service.getId())
                        .headers(getSecurityHeaders("ROLE_WORKER", station.getId(), authUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk());

        Service updated = serviceRepository.findById(service.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Новая услуга");
        assertThat(updated.getPrice().compareTo(new BigDecimal("200.00"))).isEqualTo(0);

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(rabbitTemplate).send(
                        eq(stationExchange), eq(servicesUpdatedRoutingKey), any(Message.class)
                ));
    }

    @Test
    @DisplayName("DELETE /api/service/del/{id}: Удаление услуги админом и асинхронный триггер RabbitMQ")
    void shouldDeleteServiceAndSendRabbitMessage() throws Exception {
        Station station = createAndSaveStation("Станция 1");
        Service service = createAndSaveService("Услуга для удаления", new BigDecimal("50.00"), station);

        mockMvc.perform(delete("/api/service/del/{id}", service.getId())
                        .headers(getSecurityHeaders("ROLE_ADMIN", station.getId(), authUserId)))
                .andExpect(status().isOk());

        assertThat(serviceRepository.findById(service.getId())).isEmpty();

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(rabbitTemplate).send(
                        eq(stationExchange), eq(servicesUpdatedRoutingKey), any(Message.class)
                ));
    }

    @Test
    @DisplayName("GET /api/service/getAll/{stationId}: Клиент получает список по PathVariable")
    void shouldGetAllServicesForRoleUser() throws Exception {
        Station station = createAndSaveStation("Станция Клиента");
        createAndSaveService("Мойка Кузова", new BigDecimal("30.00"), station);

        mockMvc.perform(get("/api/service/getAll/{stationId}", station.getId())
                        .headers(getSecurityHeaders("ROLE_USER", 999L, authUserId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Мойка Кузова"));
    }

    @Test
    @DisplayName("GET /api/service/getAll: Работник получает список своей станции (из токена)")
    void shouldGetAllServicesForWorker() throws Exception {
        Station station = createAndSaveStation("Станция Работника");
        createAndSaveService("Шиномонтаж", new BigDecimal("70.00"), station);

        mockMvc.perform(get("/api/service/getAll")
                        .headers(getSecurityHeaders("ROLE_WORKER", station.getId(), authUserId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Шиномонтаж"));
    }

    @Test
    @DisplayName("GET /api/service/get/{id}: Получение одной услуги по ID")
    void shouldGetServiceById() throws Exception {
        Station station = createAndSaveStation("Центральная");
        Service service = createAndSaveService("Диагностика двигателя", new BigDecimal("250.00"), station);

        mockMvc.perform(get("/api/service/get/{id}", service.getId())
                        .headers(getSecurityHeaders("ROLE_USER", station.getId(), authUserId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(service.getId()))
                .andExpect(jsonPath("$.name").value("Диагностика двигателя"));
    }

    @Test
    @DisplayName("GET /api/service/internal/{stationId}/validate: Валидация списка услуг (с телом GET-запроса)")
    void shouldValidateServicesAndCheckStation() throws Exception {
        Station station = createAndSaveStation("Проверочная");
        Service service1 = createAndSaveService("Услуга А", new BigDecimal("10"), station);
        Service service2 = createAndSaveService("Услуга Б", new BigDecimal("20"), station);

        List<Long> ids = List.of(service1.getId(), service2.getId());

        mockMvc.perform(post("/api/service/internal/{stationId}/validate", station.getId())
                        .headers(getSecurityHeaders("ROLE_SYSTEM", station.getId(), authUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ids)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationExists").value(true))
                .andExpect(jsonPath("$.services", hasSize(2)));
    }

    private Station createAndSaveStation(String name) {
        Station station = new Station();
        station.setName(name);
        station.setAddressText(name + " ул. Ленина");
        station.setLatitude(new BigDecimal("53.900000"));
        station.setLongitude(new BigDecimal("27.566700"));
        return stationRepository.save(station);
    }

    private Service createAndSaveService(String name, BigDecimal price, Station station) {
        Service service = new Service();
        service.setName(name);
        service.setPrice(price);
        service.setStation(station);
        return serviceRepository.save(service);
    }
}