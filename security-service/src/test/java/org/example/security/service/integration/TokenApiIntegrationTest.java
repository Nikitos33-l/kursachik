package org.example.security.service.integration;

import jakarta.servlet.http.Cookie;
import org.example.security.service.entity.Role;
import org.example.security.service.entity.User;
import org.example.security.service.service.BlackListService;
import org.example.security.service.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class TokenApiIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private BlackListService blackListService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private User testUser;
    private String validAccessToken;
    private String validRefreshToken;

    @BeforeEach
    void setUp() {
        Role role = roleRepository.findByName("CLIENT")
                .orElseGet(() -> roleRepository.save(new Role(null, "CLIENT")));

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test.token@example.com")
                .password("encoded_secure_password")
                .role(role)
                .workplaceId(null)
                .build();

        testUser = userRepository.save(testUser);

        validAccessToken = jwtService.createAccessToken(
                testUser.getId(),
                testUser.getEmail(),
                role.getName(),
                testUser.getWorkplaceId()
        );
        validRefreshToken = jwtService.createRefreshToken(testUser.getEmail());
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();

        if (redisTemplate.getConnectionFactory() != null) {
            redisTemplate.getConnectionFactory()
                    .getConnection()
                    .serverCommands()
                    .flushDb();
        }
    }

    @Test
    @DisplayName("Должен вернуть valid = true при передаче корректного access токена")
    void shouldReturnValidResultWhenTokenIsValid() throws Exception {
        mockMvc.perform(post("/api/token/internal/validate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validAccessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isValid", is(true)))
                .andExpect(jsonPath("$.email", is(testUser.getEmail())))
                .andExpect(jsonPath("$.userId", is(testUser.getId().toString())));
    }

    @Test
    @DisplayName("Должен вернуть valid = false, если заголовок Authorization пуст или отсутствует")
    void shouldReturnInvalidResultWhenHeaderIsMissing() throws Exception {
        mockMvc.perform(post("/api/token/internal/validate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isValid", is(false)));
    }

    @Test
    @DisplayName("Должен вернуть valid = false, если токен изменен или поврежден")
    void shouldReturnInvalidResultWhenTokenIsTampered() throws Exception {
        mockMvc.perform(post("/api/token/internal/validate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validAccessToken + "_corrupted")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isValid", is(false)));
    }

    @Test
    @DisplayName("Должен вернуть valid = false, если пользователь заблокирован (в блэклисте Redis)")
    void shouldReturnInvalidResultWhenUserIsBlacklisted() throws Exception {
        blackListService.blacklistUser(testUser.getId().toString());

        mockMvc.perform(post("/api/token/internal/validate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validAccessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isValid", is(false)));
    }

    @Test
    @DisplayName("Должен обновить и вернуть новые токены при валидной куке Refreshtoken")
    void shouldReturnNewTokensWhenRefreshTokenIsValid() throws Exception {
        Cookie refreshCookie = new Cookie("Refreshtoken", validRefreshToken);

        mockMvc.perform(post("/api/token/refresh")
                        .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(cookie().exists("Refreshtoken"))
                .andExpect(cookie().httpOnly("Refreshtoken", true))
                .andExpect(cookie().path("Refreshtoken", "/api/token/refresh"));
    }

    @Test
    @DisplayName("Должен вернуть 401 Unauthorized, если кука Refreshtoken отсутствует в запросе")
    void shouldReturnUnauthorizedWhenCookieIsMissing() throws Exception {
        mockMvc.perform(post("/api/token/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Должен вернуть 401 Unauthorized, если передан невалидный или протухший refresh токен")
    void shouldReturnUnauthorizedWhenRefreshTokenIsInvalid() throws Exception {
        Cookie badCookie = new Cookie("Refreshtoken", "completely.invalid.jwt");

        mockMvc.perform(post("/api/token/refresh")
                        .cookie(badCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Должен предотвратить повторное использование токена и вернуть 401 (Защита от кражи токенов)")
    void shouldRevokeRefreshTokenAfterFirstUse() throws Exception {
        Cookie refreshCookie = new Cookie("Refreshtoken", validRefreshToken);

        mockMvc.perform(post("/api/token/refresh")
                        .cookie(refreshCookie))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/token/refresh")
                        .cookie(refreshCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Должен вернуть 401, если пользователь был удален из базы данных")
    void shouldReturnUnauthorizedIfUserNoLongerExistsInDb() throws Exception {
        Cookie refreshCookie = new Cookie("Refreshtoken", validRefreshToken);

        userRepository.delete(testUser);

        mockMvc.perform(post("/api/token/refresh")
                        .cookie(refreshCookie))
                .andExpect(status().isUnauthorized());
    }
}