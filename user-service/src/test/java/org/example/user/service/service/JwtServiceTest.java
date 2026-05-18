package org.example.user.service.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.UUID; // Импортируем UUID

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private final String secret = Base64.getEncoder().encodeToString("super-secret-key-that-is-at-least-32-bytes-long".getBytes());

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(secret);
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

        long diff = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertEquals(1000L * 60 * 60 * 24 * 5, diff);
    }

    @Test
    @DisplayName("Ошибка при создании сервиса с невалидным Base64 секретом")
    void constructor_ShouldThrowExceptionOnInvalidSecret() {
        assertThrows(Exception.class, () -> new JwtService("not-base64-content!"));
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}