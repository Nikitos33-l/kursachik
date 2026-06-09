package org.example.user.service.integration;

import org.awaitility.Awaitility;
import org.example.user.api.requestDto.OrderUserMappingRequest;
import org.example.user.contracts.UserRegisterEvent;
import org.example.user.service.dto.request.RequestAddUserDto;
import org.example.user.service.dto.request.RequestUpdateUserDto;
import org.example.user.service.entity.Role;
import org.example.user.service.entity.User;
import org.example.user.service.entity.Vehicle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserApiIntegrationTests extends BaseIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${user.queue.registration}")
    private String registrationQueue;

    @Value("${station.delete.queue}")
    private String stationDeleteQueue;

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

        mockMvc.perform(delete("/api/user/delete/{id}", savedUser.getId())
                        .headers(getSecurityHeaders("ROLE_ADMIN", 100L, authUserId)))
                .andExpect(status().isOk());

        assertThat(userRepository.findById(savedUser.getId())).isEmpty();
    }

    @Test
    @DisplayName("Успешное добавление нового пользователя супер-админом")
    void shouldAddUserBySuperAdmin() throws Exception {
        createAndSaveRole("ADMIN");

        RequestAddUserDto dto = new RequestAddUserDto("Иван", "ivan@gmail.com", "pass", "ADMIN", 12L);

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

        RequestUpdateUserDto invalidDto = new RequestUpdateUserDto("", "");

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
        Role adminRole = createAndSaveRole("ADMIN");

        User worker = User.builder()
                .id(UUID.randomUUID())
                .email("worker@mail.com").name("Vasya").workplaceId(100L).role(workerRole).build();
        User admin = User.builder()
                .id(UUID.randomUUID())
                .email("admin@mail.com").name("Petr").workplaceId(100L).role(adminRole).build();

        userRepository.saveAll(List.of(worker, admin));

        mockMvc.perform(get("/api/user/get/all/workers")
                        .headers(getSecurityHeaders("ROLE_ADMIN", 100L, authUserId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Vasya"));
    }

    @Test
    @DisplayName("Успешное получение краткой информации о пользователе по его ID")
    void shouldGetUserInfo() throws Exception {
        User savedUser = createAndSaveTestUser(100L, "Ivan");

        mockMvc.perform(get("/api/user/get/info/{id}", savedUser.getId())
                        .headers(getSecurityHeaders("ROLE_ADMIN",100L,authUserId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ivan"))
                .andExpect(jsonPath("$.id").value(savedUser.getId().toString()));
    }

    @Test
    @DisplayName("Успешное обновление данных пользователя (валидный сценарий)")
    void shouldUpdateUserSuccessfully() throws Exception {
        User savedUser = createAndSaveTestUser(100L, "OldName");

        RequestUpdateUserDto updateDto = new RequestUpdateUserDto("NewName", "newemail@mail.com");

        mockMvc.perform(put("/api/user/update/{id}", savedUser.getId())
                        .headers(getSecurityHeaders("ROLE_ADMIN", 100L, authUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk());

        User updatedUser = userRepository.findById(savedUser.getId()).orElseThrow();
        assertThat(updatedUser.getName()).isEqualTo("NewName");
        assertThat(updatedUser.getEmail()).isEqualTo("newemail@mail.com");
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

        mockMvc.perform(post("/api/user/internal/get/orderInfo")
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

        mockMvc.perform(post("/api/user/internal/getAll/orderInfo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestList)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['10']").exists());
    }

    @Test
    @DisplayName("Internal: Успешная валидация сотрудников по ID без заголовков авторизации")
    void shouldValidateWorkersInternal() throws Exception {
        User worker1 = User.builder().id(UUID.randomUUID()).email("worker1@mail.com").name("Ivan").build();
        User worker2 = User.builder().id(UUID.randomUUID()).email("worker2@mail.com").name("Petr").build();
        userRepository.saveAll(List.of(worker1, worker2));

        mockMvc.perform(post("/api/user/internal/validate-workers")
                        .param("ids", worker1.getId().toString(), worker2.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.emails.['" + worker1.getId() + "']").value("worker1@mail.com"))
                .andExpect(jsonPath("$.emails.['" + worker2.getId() + "']").value("worker2@mail.com"));
    }
    @Test
    @DisplayName("RabbitMQ: Успешная обработка события регистрации пользователя (UserEventConsumer)")
    void shouldHandleUserRegisterEvent() throws InterruptedException {
        createAndSaveRole("WORKER");
        UUID newUserId = UUID.randomUUID();
        UserRegisterEvent event = new UserRegisterEvent(newUserId, "rabbit@mail.com", "RabbitWorker", "hash", "WORKER", 99L);

        rabbitTemplate.convertAndSend(registrationQueue, event);

        Awaitility.await().atMost(5, TimeUnit.SECONDS).
                pollInterval(200,TimeUnit.MILLISECONDS)
                .untilAsserted(()->{
                    assertThat(userRepository.findById(newUserId)).isPresent();
                    assertThat(userRepository.findById(newUserId).get().getName()).isEqualTo("RabbitWorker");
                });

    }

    @Test
    @DisplayName("RabbitMQ: Успешное удаление сотрудников при удалении автостанции")
    void shouldHandleStationDeleteEvent() throws InterruptedException {
        createAndSaveTestUser(55L, "WorkerOnStation55");
        User userToKeep = createAndSaveTestUser(77L, "WorkerOnStation77");

        rabbitTemplate.convertAndSend(stationDeleteQueue, 55L);

        Awaitility.await().atMost(5,TimeUnit.SECONDS).
                pollInterval(200,TimeUnit.MILLISECONDS).
                untilAsserted(()->{
                    List<User> remainingUsers = userRepository.findAll();
                    assertThat(remainingUsers).hasSize(1);
                    assertThat(remainingUsers.get(0).getId()).isEqualTo(userToKeep.getId());
                });
    }

    private User createAndSaveTestUser(Long stationId, String name) {
        Role role = roleRepository.findByName("WORKER").orElseGet(() -> createAndSaveRole("WORKER"));
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("test_" + name.toLowerCase() + "@mail.com")
                .name(name)
                .role(role)
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