package org.example.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.order.service.api.common.client.OrderServiceClient;
import org.example.user.service.dto.request.RequestAddUserDto;
import org.example.user.service.dto.request.RequestUpdateUserDto;
import org.example.user.service.entity.Role;
import org.example.user.service.entity.User;
import org.example.user.service.repository.RoleRepository;
import org.example.user.service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
class UserServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoleRepository roleRepository;

    @MockitoBean
    private OrderServiceClient orderServiceClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @ServiceConnection
    @Container
    static PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:18.4-alpine");

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();
    }

    @Test
    @DisplayName("Успешное получение всех пользователей админом")
    void shouldGetAllUsersForAdmin() throws Exception {
        createAndSaveTestUser(100L, "Ivan");

        mockMvc.perform(get("/api/user/getAll")
                        .headers(getSecurityHeaders("ROLE_ADMIN", 100L, 1L))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Ivan"));
    }

    @Test
    @DisplayName("Отказ в доступе при попытке получить всех пользователей без роли ADMIN")
    void shouldFailGetAllUsersWithoutAdminRole() throws Exception {
        mockMvc.perform(get("/api/user/getAll")
                        .headers(getSecurityHeaders("ROLE_WORKER", 100L, 2L))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Успешное удаление пользователя")
    void shouldDeleteUser() throws Exception {
        User savedUser = createAndSaveTestUser(100L, "Oleg");
        doNothing().when(orderServiceClient).deleteByUser(savedUser.getId());

        mockMvc.perform(delete("/api/user/delete/{id}", savedUser.getId())
                        .headers(getSecurityHeaders("ROLE_ADMIN", 100L, 1L)))
                .andExpect(status().isOk());

        assertThat(userRepository.findById(savedUser.getId())).isEmpty();
    }

    @Test
    @DisplayName("Успешное добавление нового пользователя супер-админом")
    void shouldAddUserBySuperAdmin() throws Exception {
        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setName("ADMIN");
        roleRepository.save(adminRole);

        RequestAddUserDto dto = new RequestAddUserDto("Иван","ivan@gmail.com","pass","ADMIN",12L);

        mockMvc.perform(post("/api/user/add")
                        .headers(getSecurityHeaders("ROLE_SUPERADMIN", 100L, 1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        assertThat(userRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("При добавлении пользователя обычным ADMIN-ом, stationId должен браться из заголовка ADMIN-а")
    void shouldAddUserByAdminUsingAdminsStationId() throws Exception {
        Role workerRole = new Role();
        workerRole.setId(2L);
        workerRole.setName("WORKER");
        roleRepository.save(workerRole);

        RequestAddUserDto dto = new RequestAddUserDto("WorkerName", "worker@gmail.com", "pass", "WORKER", 12L);

        mockMvc.perform(post("/api/user/add")
                        .headers(getSecurityHeaders("ROLE_ADMIN", 100L, 1L))
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
        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setName("ADMIN");
        roleRepository.save(adminRole);

        createAndSaveTestUser(100L, "Ivan");

        RequestAddUserDto duplicateDto = new RequestAddUserDto("Ivan2", "test_ivan@mail.com", "pass", "ADMIN", 100L);

        mockMvc.perform(post("/api/user/add")
                        .headers(getSecurityHeaders("ROLE_SUPERADMIN", 100L, 1L))
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
                        .headers(getSecurityHeaders("ROLE_ADMIN", 100L, 1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Успешное получение только сотрудников (WORKER) конкретной станции")
    void shouldGetAllWorkersForStation() throws Exception {
        Role workerRole = new Role();
        workerRole.setId(2L);
        workerRole.setName("WORKER");
        roleRepository.save(workerRole);

        User worker = User.builder()
                .email("worker@mail.com").name("Vasya").workplaceId(100L).role(workerRole).build();
        User admin = User.builder()
                .email("admin@mail.com").name("Petr").workplaceId(100L).build();

        userRepository.saveAll(List.of(worker, admin));

        mockMvc.perform(get("/api/user/get/all/workers")
                        .headers(getSecurityHeaders("ROLE_ADMIN", 100L, 1L))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Vasya"));
    }


    private HttpHeaders getSecurityHeaders(String role, Long stationId, Long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", String.valueOf(userId));
        headers.add("X-Station-Id", stationId != null ? String.valueOf(stationId) : "");
        headers.add("X-User-Roles", role);
        headers.add("X-User-Email", "test@mail.com");
        return headers;
    }

    private User createAndSaveTestUser(Long stationId, String name) {
        User user = User.builder()
                .email("test_" + name.toLowerCase() + "@mail.com")
                .name(name)
                .workplaceId(stationId)
                .build();
        return userRepository.save(user);
    }
}