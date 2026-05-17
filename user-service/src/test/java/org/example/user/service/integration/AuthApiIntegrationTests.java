package org.example.user.service.integration;

import org.example.user.service.dto.request.LoginRequest;
import org.example.user.service.dto.request.RegisterRequest;
import org.example.user.service.entity.Role;
import org.example.user.service.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthApiIntegrationTests extends BaseIntegrationTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Успешная регистрация нового клиента, проверка JWT в теле и HttpOnly Cookie")
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

        User savedUser = userRepository.findByEmail("alex@mail.com");
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getName()).isEqualTo("Aleksey");
        assertThat(savedUser.getRole().getName()).isEqualTo("CLIENT");
        assertThat(passwordEncoder.matches("strongPassword123", savedUser.getPassword())).isTrue();
    }

    @Test
    @DisplayName("Ошибка 409 при регистрации, если email уже занят")
    void shouldReturnConflictWhenRegisteringExistingEmail() throws Exception {
        Role clientRole = createAndSaveRole("CLIENT");
        createAndSaveAuthUser("OldUser", "duplicate@mail.com", "anyPass", clientRole);

        RegisterRequest duplicateRequest = new RegisterRequest("NewUser", "duplicate@mail.com", "password");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Успешная авторизация пользователя с получением токенов")
    void shouldLoginSuccessfullyWithCorrectCredentials() throws Exception {
        Role clientRole = createAndSaveRole("CLIENT");
        createAndSaveAuthUser("Dmitry", "dima@mail.com", "correct_password", clientRole);

        LoginRequest loginRequest = new LoginRequest("dima@mail.com", "correct_password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(cookie().exists("Refreshtoken"))
                .andExpect(cookie().httpOnly("Refreshtoken", true));
    }

    @Test
    @DisplayName("Ошибка 401 (BadCredentialsException) при неверном пароле")
    void shouldReturnUnauthorizedWithWrongPassword() throws Exception {
        Role clientRole = createAndSaveRole("CLIENT");
        createAndSaveAuthUser("Dmitry", "dima@mail.com", "correct_password", clientRole);

        LoginRequest wrongLoginRequest = new LoginRequest("dima@mail.com", "wrong_password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongLoginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Ошибка 401 при попытке входа с несуществующим email")
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

    private User createAndSaveAuthUser(String name, String email, String plainPassword, Role role) {
        User user = User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(plainPassword))
                .role(role)
                .build();
        return userRepository.save(user);
    }
}