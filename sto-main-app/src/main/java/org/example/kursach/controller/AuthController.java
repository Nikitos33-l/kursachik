package org.example.kursach.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.example.kursach.entity.User;
import org.example.kursach.service.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String,String>> create(@RequestBody User user, HttpServletResponse response){
        Map<String, String> map = authService.save(user);
        ResponseCookie cookie = ResponseCookie.from("Refreshtoken", map.get("Refreshtoken"))
                .httpOnly(true)
                .path("/api/token/refresh")
                .sameSite("None")
                .secure(false)
                .maxAge(Duration.ofDays(5))
                .build();
        System.out.println("Это кука: " + cookie);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(map);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String,String>> login(@RequestBody User user,HttpServletResponse response) {
        Map<String, String> map = authService.login(user);
        ResponseCookie cookie = ResponseCookie.from("Refreshtoken", map.get("Refreshtoken"))
                .httpOnly(true)
                .secure(false)
                .path("/api/token/refresh")
                .sameSite("None")
                .maxAge(Duration.ofDays(5))
                .build();
        System.out.println("Это кука: " + cookie);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok().body(map);
    }
}
