package org.example.security.service.integration;

import org.example.security.service.dto.request.LoginRequest;
import org.example.security.service.dto.request.RegisterRequest;
import org.example.security.service.entity.Role;
import org.example.security.service.entity.User;
import org.example.user.contracts.UserRegisterEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthApiIntegrationTests extends BaseIntegrationTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoSpyBean
    private RabbitTemplate rabbitTemplate;

    @Value("${user.exchange.name}")
    private String userExchange;

    @Value("${user.register.routing.key}")
    private String userRegisterRoutingKey;

    @Test
    @DisplayName("POST /api/auth/register: Успешная регистрация, проверка записи в БД, Cookies и отправки в RabbitMQ")
    void shouldRegisterNewClientSuccessfully() throws Exception {
        createAndSaveRole("CLIENT");
        RegisterRequest registerRequest = new RegisterRequest("Aleksey", "alex@mail.com", "strongPassword123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(cookie().exists("Refreshtoken"))
                .andExpect(cookie().httpOnly("Refreshtoken", true))
                .andExpect(cookie().path("Refreshtoken", "/api/token/refresh"));

        User savedUser = userRepository.findByEmail("alex@mail.com").orElseThrow();
        assertThat(savedUser.getRole().getName()).isEqualTo("CLIENT");
        assertThat(passwordEncoder.matches("strongPassword123", savedUser.getPassword())).isTrue();

        verify(rabbitTemplate, timeout(2000)).convertAndSend(
                eq(userExchange),
                eq(userRegisterRoutingKey),
                any(UserRegisterEvent.class)
        );
    }

    @Test
    @DisplayName("POST /api/auth/register: Ошибка 409, если пользователь с таким email уже существует")
    void shouldReturnConflictWhenRegisteringExistingEmail() throws Exception {
        Role clientRole = createAndSaveRole("CLIENT");
        createAndSaveAuthUser("duplicate@mail.com", "anyPass", clientRole);

        RegisterRequest duplicateRequest = new RegisterRequest("NewUser", "duplicate@mail.com", "password125");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/auth/login: Успешная авторизация пользователя, выдача JWT и HttpOnly Cookie")
    void shouldLoginSuccessfullyWithCorrectCredentials() throws Exception {
        Role clientRole = createAndSaveRole("CLIENT");
        createAndSaveAuthUser("dima@mail.com", "correct_password", clientRole);

        LoginRequest loginRequest = new LoginRequest("dima@mail.com", "correct_password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(cookie().exists("Refreshtoken"))
                .andExpect(cookie().httpOnly("Refreshtoken", true))
                .andExpect(cookie().path("Refreshtoken", "/api/token/refresh"));
    }

    @Test
    @DisplayName("POST /api/auth/login: Ошибка 401 при вводе неверного пароля")
    void shouldReturnUnauthorizedWithWrongPassword() throws Exception {
        Role clientRole = createAndSaveRole("CLIENT");
        createAndSaveAuthUser("dima@mail.com", "correct_password", clientRole);

        LoginRequest wrongLoginRequest = new LoginRequest("dima@mail.com", "wrong_password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongLoginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/login: Ошибка 401 при попытке входа с несуществующим email")
    void shouldReturnUnauthorizedWithNonExistentEmail() throws Exception {
        LoginRequest unknownUserRequest = new LoginRequest("nobody@mail.com", "anyPassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unknownUserRequest)))
                .andExpect(status().isUnauthorized());
    }


    private Role createAndSaveRole(String roleName) {
        Role role = new Role();
        role.setName(roleName);
        return roleRepository.save(role);
    }

    private User createAndSaveAuthUser(String email, String plainPassword, Role role) {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .password(passwordEncoder.encode(plainPassword))
                .role(role)
                .build();
        return userRepository.save(user);
    }
}