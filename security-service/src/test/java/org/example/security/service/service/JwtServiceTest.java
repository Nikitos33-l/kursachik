package org.example.security.service.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.example.security.service.api.common.dto.TokenValidationResultDto;
import org.example.security.service.dto.response.TokenPair;
import org.example.security.service.entity.Role;
import org.example.security.service.entity.User;
import org.example.security.service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private BlackListService blacklistService;
    @Mock
    private UserRepository userRepository;

    private JwtService jwtService;

    private final String secret = Base64.getEncoder().encodeToString("super-secret-key-that-is-at-least-32-bytes-long".getBytes());

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(secret, blacklistService, userRepository);
    }

    @Test
    @DisplayName("Создание Access Token с корректными клеймами")
    void createAccessToken_ShouldContainCorrectClaims() {
        UUID userId = UUID.randomUUID();
        String email = "test@mail.com";
        String role = "ROLE_ADMIN";
        Long stationId = 10L;

        String token = jwtService.createAccessToken(userId, email, role, stationId);

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertEquals(email, claims.getSubject());
        assertEquals(userId.toString(), claims.get("userId", String.class));
        assertEquals(role, claims.get("role", String.class));
        assertEquals(stationId, claims.get("stationId", Long.class));
        assertNotNull(claims.getId()); // Проверка JTI (ID токена)

        assertTrue(claims.getExpiration().after(new Date()));
        long diff = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertEquals(1000 * 60 * 15, diff);
    }

    @Test
    @DisplayName("Создание Refresh Token с корректным subject")
    void createRefreshToken_ShouldHaveCorrectSubject() {
        String email = "refresh@mail.com";

        String token = jwtService.createRefreshToken(email);

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertEquals(email, claims.getSubject());
        assertNull(claims.get("role"));
        assertNotNull(claims.getId());

        long diff = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertEquals(1000L * 60 * 60 * 24 * 5, diff); // 5 дней
    }

    @Test
    @DisplayName("Успешная валидация корректного токена")
    void validateToken_ValidToken_ReturnsValidResult() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.createAccessToken(userId, "test@mail.com", "ROLE_USER", 5L);

        when(blacklistService.isUserBlacklisted(userId.toString())).thenReturn(false);

        TokenValidationResultDto result = jwtService.validateToken(token);

        assertTrue(result.isValid());
        assertEquals(userId, result.userId());
        assertEquals("test@mail.com", result.email());
        assertEquals("ROLE_USER", result.roles().get(0));
        assertEquals(5L, result.stationId());
    }

    @Test
    @DisplayName("Валидация провалена: пользователь находится в блэклисте")
    void validateToken_UserIsBlacklisted_ReturnsInvalidResult() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.createAccessToken(userId, "bad_user@mail.com", "ROLE_USER", null);

        when(blacklistService.isUserBlacklisted(userId.toString())).thenReturn(true);

        TokenValidationResultDto result = jwtService.validateToken(token);

        assertFalse(result.isValid());
        assertNull(result.userId());
    }

    @Test
    @DisplayName("Валидация провалена: испорченный токен")
    void validateToken_InvalidToken_ReturnsInvalidResult() {
        TokenValidationResultDto result = jwtService.validateToken("ey.garbage.token");
        assertFalse(result.isValid());
    }

    @Test
    @DisplayName("Успешное обновление токенов (Refresh)")
    void refreshTokens_Success() {
        String email = "valid@mail.com";
        String refreshToken = jwtService.createRefreshToken(email);

        Role mockRole = new Role();
        mockRole.setName("ROLE_WORKER");

        User mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setEmail(email);
        mockUser.setRole(mockRole);
        mockUser.setWorkplaceId(12L);

        when(blacklistService.isTokenRevoked(anyString())).thenReturn(false);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));

        TokenPair tokenPair = jwtService.refreshTokens(refreshToken);

        assertNotNull(tokenPair.accessToken());
        assertNotNull(tokenPair.refreshToken());

        verify(blacklistService, times(1)).blacklistToken(anyString(), anyLong());
    }

    @Test
    @DisplayName("Обновление провалено: Refresh Token был отозван (в блэклисте)")
    void refreshTokens_TokenRevoked_ThrowsException() {
        String refreshToken = jwtService.createRefreshToken("hacker@mail.com");

        when(blacklistService.isTokenRevoked(anyString())).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> jwtService.refreshTokens(refreshToken));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Token has been revoked. Possible theft detected!", exception.getReason());
    }

    @Test
    @DisplayName("Обновление провалено: пользователь удален из БД")
    void refreshTokens_UserNotFound_ThrowsException() {
        String refreshToken = jwtService.createRefreshToken("ghost@mail.com");

        when(blacklistService.isTokenRevoked(anyString())).thenReturn(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> jwtService.refreshTokens(refreshToken));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("User no longer exists", exception.getReason());
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}