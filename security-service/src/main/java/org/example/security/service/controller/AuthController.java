package org.example.security.service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.security.service.dto.request.LoginRequest;
import org.example.security.service.dto.request.RegisterRequest;
import org.example.security.service.dto.response.TokenPair;
import org.example.security.service.service.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Аутентификация и регистрация", description = "Точки входа для регистрации пользователей и генерации первичных токенов доступа")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Регистрация нового пользователя", description = "Создает аккаунт, генерирует токены. Refresh-токен помещается в безопасную HttpOnly куку.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешная регистрация"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации входных данных или email уже занят")
    })
    public ResponseEntity<Map<String, String>> register(
            @Valid @RequestBody RegisterRequest request,
            @Parameter(hidden = true) HttpServletResponse response
    ) {
        log.info("Попытка регистрации нового пользователя с email: {}", request.email());

        TokenPair tokens = authService.register(request);
        setRefreshCookie(response, tokens.refreshToken());

        log.info("Пользователь [{}] успешно зарегистрирован", request.email());
        return ResponseEntity.ok(Map.of("accessToken", tokens.accessToken()));
    }

    @PostMapping("/login")
    @Operation(summary = "Аутентификация (Вход в систему)", description = "Проверяет учетные данные пользователя. Возвращает Access Token в теле, а Refresh — в HttpOnly куке.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешная аутентификация"),
            @ApiResponse(responseCode = "401", description = "Неверный логин или пароль")
    })
    public ResponseEntity<Map<String, String>> login(
            @Valid @RequestBody LoginRequest request,
            @Parameter(hidden = true) HttpServletResponse response
    ) {
        log.info("Запрос на вход в систему для пользователя: {}", request.email());

        TokenPair tokens = authService.login(request);
        setRefreshCookie(response, tokens.refreshToken());

        log.info("Пользователь [{}] успешно авторизован", request.email());
        return ResponseEntity.ok(Map.of("accessToken", tokens.accessToken()));
    }

    private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
        log.debug("Установка HttpOnly куки 'Refreshtoken'");
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