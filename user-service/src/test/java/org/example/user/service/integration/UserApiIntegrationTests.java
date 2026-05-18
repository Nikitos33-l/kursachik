package org.example.user.service.integration;

import org.example.order.service.api.common.client.OrderServiceClient;
import org.example.user.api.requestDto.OrderUserMappingRequest;
import org.example.user.service.dto.request.RequestAddUserDto;
import org.example.user.service.dto.request.RequestUpdateUserDto;
import org.example.user.service.entity.Role;
import org.example.user.service.entity.User;
import org.example.user.service.entity.Vehicle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserApiIntegrationTests extends BaseIntegrationTest {

    @MockitoBean
    OrderServiceClient orderServiceClient;

    private final UUID authUserId = UUID.randomUUID();

    @Test
    @DisplayName("Успешное получение всех пользователей админом")
    void shouldGetAllUsersForAdmin() throws Exception {
        createAndSaveTestUser(100L, "Ivan");

        mockMvc.perform(get("/api/user/getAll")
                        .headers(getSecurityHeaders("ROLE_ADMIN", 100L, authUserId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Ivan"));
    }

    @Test
    @DisplayName("Отказ в доступе при попытке получить всех пользователей без роли ADMIN")
    void shouldFailGetAllUsersWithoutAdminRole() throws Exception {
        mockMvc.perform(get("/api/user/getAll")
                        .headers(getSecurityHeaders("ROLE_WORKER", 100L, authUserId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Успешное удаление пользователя")
    void shouldDeleteUser() throws Exception {
        User savedUser = createAndSaveTestUser(100L, "Oleg");
        doNothing().when(orderServiceClient).deleteByUser(savedUser.getId());

        mockMvc.perform(delete("/api/user/delete/{id}", savedUser.getId())
                        .headers(getSecurityHeaders("ROLE_ADMIN", 100L, authUserId)))
                .andExpect(status().isOk());

        assertThat(userRepository.findById(savedUser.getId())).isEmpty();
    }

    @Test
    @DisplayName("Успешное добавление нового пользователя супер-админом")
    void shouldAddUserBySuperAdmin() throws Exception {
        createAndSaveRole("ADMIN");

        RequestAddUserDto dto = new RequestAddUserDto("Иван","ivan@gmail.com","pass","ADMIN",12L);

        mockMvc.perform(post("/api/user/add")
                        .headers(getSecurityHeaders("ROLE_SUPERADMIN", 100L, authUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        assertThat(userRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("При добавлении пользователя обычным ADMIN-ом, stationId должен браться из заголовка ADMIN-а")
    void shouldAddUserByAdminUsingAdminsStationId() throws Exception {
        createAndSaveRole("WORKER");

        RequestAddUserDto dto = new RequestAddUserDto("WorkerName", "worker@gmail.com", "pass", "WORKER", 12L);

        mockMvc.perform(post("/api/user/add")
                        .headers(getSecurityHeaders("ROLE_ADMIN", 100L, authUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        List<User> users = userRepository.findAll();
        assertThat(users).hasSize(1);
        assertThat(users.get(0).getWorkplaceId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Ошибка 409 (IllegalStateException) при попытке добавить пользователя с уже существующим email")
    void shouldFailWhenUserWithEmailAlreadyExists() throws Exception {
        createAndSaveRole("ADMIN");
        createAndSaveTestUser(100L, "Ivan");

        RequestAddUserDto duplicateDto = new RequestAddUserDto("Ivan2", "test_ivan@mail.com", "pass", "ADMIN", 100L);

        mockMvc.perform(post("/api/user/add")
                        .headers(getSecurityHeaders("ROLE_SUPERADMIN", 100L, authUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateDto)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Ошибка 400 при обновлении пользователя с невалидными данными")
    void shouldReturnBadRequestOnInvalidUpdate() throws Exception {
        User savedUser = createAndSaveTestUser(100L, "Anna");

        RequestUpdateUserDto invalidDto = new RequestUpdateUserDto("","");

        mockMvc.perform(put("/api/user/update/{id}", savedUser.getId())
                        .headers(getSecurityHeaders("ROLE_ADMIN", 100L, authUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Успешное получение только сотрудников (WORKER) конкретной станции")
    void shouldGetAllWorkersForStation() throws Exception {
        Role workerRole = createAndSaveRole("WORKER");

        // Здесь тоже добавил явную генерацию ID для обоих пользователей
        User worker = User.builder()
                .id(UUID.randomUUID())
                .email("worker@mail.com").name("Vasya").workplaceId(100L).role(workerRole).build();
        User admin = User.builder()
                .id(UUID.randomUUID())
                .email("admin@mail.com").name("Petr").workplaceId(100L).build();

        userRepository.saveAll(List.of(worker, admin));

        mockMvc.perform(get("/api/user/get/all/workers")
                        .headers(getSecurityHeaders("ROLE_ADMIN", 100L, authUserId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Vasya"));
    }

    @Test
    @DisplayName("Internal: Успешное получение информации для одного заказа без заголовков авторизации")
    void shouldGetOrderInfoInternal() throws Exception {
        User savedUser = createAndSaveTestUser(100L, "Vasya");
        Vehicle savedVehicle = createAndSaveVehicle("Toyota", "Camry", "AA1111-7");

        OrderUserMappingRequest requestDto = new OrderUserMappingRequest(
                1L,
                savedUser.getId(),
                Set.of(savedUser.getId()),
                savedVehicle.getId()
        );

        mockMvc.perform(get("/api/user/internal/get/orderInfo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Internal: Успешное получение информации для списка заказов без заголовков авторизации")
    void shouldGetOrdersInfoInternal() throws Exception {
        User savedUser = createAndSaveTestUser(100L, "Petr");
        Vehicle savedVehicle = createAndSaveVehicle("BMW", "X5", "BB2222-7");

        List<OrderUserMappingRequest> requestList = List.of(
                new OrderUserMappingRequest(
                        10L,
                        savedUser.getId(),
                        Set.of(savedUser.getId()),
                        savedVehicle.getId()
                )
        );

        mockMvc.perform(get("/api/user/internal/getAll/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestList)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['10']").exists());
    }

    @Test
    @DisplayName("Internal: Успешная валидация сотрудников по ID без заголовков авторизации")
    void shouldValidateWorkersInternal() throws Exception {
        // Добавил генерацию ID для worker1 и worker2
        User worker1 = User.builder().id(UUID.randomUUID()).email("worker1@mail.com").name("Ivan").build();
        User worker2 = User.builder().id(UUID.randomUUID()).email("worker2@mail.com").name("Petr").build();
        userRepository.saveAll(List.of(worker1, worker2));

        mockMvc.perform(get("/api/user/internal/validate-workers")
                        .param("ids", worker1.getId().toString(), worker2.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.emails.['" + worker1.getId() + "']").value("worker1@mail.com"))
                .andExpect(jsonPath("$.emails.['" + worker2.getId() + "']").value("worker2@mail.com"));
    }

    @Test
    @DisplayName("Internal: Успешное deletion сотрудников конкретной автостанции без заголовков авторизации")
    void shouldDeleteWorkersByWorkplaceInternal() throws Exception {
        mockMvc.perform(delete("/api/user/internal/delete/by/workplace/{id}", 100L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    private User createAndSaveTestUser(Long stationId, String name) {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("test_" + name.toLowerCase() + "@mail.com")
                .name(name)
                .workplaceId(stationId)
                .build();
        return userRepository.save(user);
    }

    private Role createAndSaveRole(String roleName) {
        Role role = new Role();
        role.setName(roleName);
        return roleRepository.save(role);
    }

    private Vehicle createAndSaveVehicle(String make, String model, String number) {
        Vehicle vehicle = Vehicle.builder()
                .make(make)
                .model(model)
                .number(number)
                .build();
        return vehicleRepository.save(vehicle);
    }
}