package org.example.user.service.integration;

import org.example.user.api.requestDto.CarRequestDto;
import org.example.user.api.requestDto.OrderVehicleMappingRequest;
import org.example.user.service.entity.Vehicle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID; // Добавили импорт UUID

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CarApiIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Успешное получение информации о машинах для списка заказов через реальную БД")
    void shouldGetCarsInfoForOrdersFromDatabase() throws Exception {
        Vehicle vehicle = Vehicle.builder()
                .make("Toyota")
                .model("Camry")
                .number("AA1111-7")
                .build();
        Vehicle savedVehicle = vehicleRepository.save(vehicle);

        List<OrderVehicleMappingRequest> requestList = List.of(
                new OrderVehicleMappingRequest(1L, savedVehicle.getId())
        );

        mockMvc.perform(post("/api/cars/internal/getAll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestList)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['1']").exists())
                .andExpect(jsonPath("$['1'].id").value(savedVehicle.getId()))
                .andExpect(jsonPath("$['1'].make").value("Toyota"))
                .andExpect(jsonPath("$['1'].model").value("Camry"));
    }

    @Test
    @DisplayName("Успешное создание автомобиля в реальной БД, если его там не было")
    void shouldCreateCarInDatabaseIfNotExist() throws Exception {
        CarRequestDto carRequestDto = new CarRequestDto("BMW", "X5", "BB2222-7", UUID.randomUUID());

        assertThat(vehicleRepository.findAll()).isEmpty();

        mockMvc.perform(post("/api/cars/internal/get-or-create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(carRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.make").value("BMW"))
                .andExpect(jsonPath("$.model").value("X5"));

        List<Vehicle> savedVehicles = vehicleRepository.findAll();
        assertThat(savedVehicles).hasSize(1);
        assertThat(savedVehicles.get(0).getMake()).isEqualTo("BMW");
        assertThat(savedVehicles.get(0).getNumber()).isEqualTo("BB2222-7");
    }
}