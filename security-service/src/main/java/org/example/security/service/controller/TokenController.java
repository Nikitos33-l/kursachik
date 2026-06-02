package org.example.security.service.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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

@RestController
@RequestMapping("/api/token")
@RequiredArgsConstructor
public class TokenController {
    private final JwtService jwtService;

    @PostMapping("/internal/validate")
    TokenValidationResultDto validateToken(@RequestHeader(name = HttpHeaders.AUTHORIZATION,required = false) String authHeader){
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return TokenValidationResultDto.invalid();
        }

        String token = authHeader.substring(7);

        return jwtService.validateToken(token);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(
            @CookieValue(name = "Refreshtoken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }


        TokenPair tokens = jwtService.refreshTokens(refreshToken);

        setRefreshCookie(response, tokens.refreshToken());

        return ResponseEntity.ok(Map.of("accessToken", tokens.accessToken()));
    }

    private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
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
