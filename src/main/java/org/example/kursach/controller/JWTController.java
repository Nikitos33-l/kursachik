package org.example.kursach.controller;


import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.example.kursach.service.JWTService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController()
@RequestMapping("/api/token")
public class JWTController {
    JWTService jwtService;
    public JWTController(JWTService jwtService) {
        this.jwtService = jwtService;
    }
    @PostMapping("/refresh")
    public ResponseEntity<Map<String,String>> refresh_token(HttpServletRequest request){
//        String refresh_token = Arrays.stream(request.getCookies()).
//                filter(c->"Refreshtoken".equals(c.getName())).
//                map(Cookie::getValue).
//                findFirst().orElseThrow(() -> new RuntimeException("Refresh token not found"));
        String refresh_token = jwtService.get_token(request);
        System.out.println("кука есть: "+refresh_token);
        String acess_token = jwtService.refreshing_acess_token(refresh_token);
        Map<String,String> result_map = new HashMap<>();
        result_map.put("Acesstoken",acess_token);
        return ResponseEntity.ok(result_map);
    }
}
