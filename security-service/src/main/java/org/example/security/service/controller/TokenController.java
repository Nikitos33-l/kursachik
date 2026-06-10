package org.example.security.service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.security.service.api.common.dto.TokenValidationResultDto;
import org.example.security.service.dto.response.TokenPair;
import org.example.security.service.service.JwtService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/token")
@RequiredArgsConstructor
@Tag(name = "Управление JWT токенами", description = "Внутренняя валидация токенов для шлюза (Gateway) и механизмы обновления сессий (Refresh)")
public class TokenController {
    private final JwtService jwtService;

    @PostMapping("/internal/validate")
    @Operation(summary = "[Внутренний] Валидация Access Token", description = "Используется API Gateway или межсервисными механизмами для проверки подписи и сроков действия токена.")
    public TokenValidationResultDto validateToken(
            @Parameter(description = "Заголовок Authorization, содержащий 'Bearer <token>'")
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        // Используем DEBUG, чтобы логи не забивались на каждый чих фронтенда
        log.debug("Получен внутренний запрос на валидацию токена");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Валидация провалена: Отсутствует или некорректен заголовок Authorization");
            return TokenValidationResultDto.invalid();
        }

        String token = authHeader.substring(7);
        TokenValidationResultDto result = jwtService.validateToken(token);

        log.debug("Результат валидации токена: действителен = {}", result.isValid());
        return result;
    }

    @PostMapping("/refresh")
    @Operation(summary = "Обновить Access Token", description = "Извлекает Refresh Token из защищенной куки и генерирует новую пару токенов.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Токены успешно перевыпущены"),
            @ApiResponse(responseCode = "401", description = "Кука отсутствует или срок действия Refresh-токена истек")
    })
    public ResponseEntity<Map<String, String>> refresh(
            @Parameter(description = "Токен обновления, передаваемый в куках автоматически")
            @CookieValue(name = "Refreshtoken", required = false) String refreshToken,
            @Parameter(hidden = true) HttpServletResponse response
    ) {
        log.info("Запрос на ротацию (refresh) токенов через Cookie");

        if (refreshToken == null) {
            log.warn("Отказ в обновлении токена: Кука 'Refreshtoken' не найдена в запросе");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        TokenPair tokens = jwtService.refreshTokens(refreshToken);
        setRefreshCookie(response, tokens.refreshToken());

        log.info("Сессия успешно продлена, выдан новый Access Token");
        return ResponseEntity.ok(Map.of("accessToken", tokens.accessToken()));
    }

    private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
        log.debug("Обновление HttpOnly куки 'Refreshtoken'");
        ResponseCookie cookie = ResponseCookie.from("Refreshtoken", refreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/api/token/refresh")
                .maxAge(Duration.ofDays(5))
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}