package org.example.security.service.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.security.service.dto.request.LoginRequest;
import org.example.security.service.dto.request.RegisterRequest;
import org.example.security.service.dto.response.TokenPair;
import org.example.security.service.entity.User;
import org.example.security.service.repository.RoleRepository;
import org.example.security.service.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;

    @Transactional
    public TokenPair login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email()).orElseThrow(()->new BadCredentialsException("Неверный email или пароль"));

        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Неверный email или пароль");
        }

        Long stationId = user.getWorkplaceId();

        return generateTokenPair(user.getId(),user.getEmail(), user.getRole().getName(), stationId);
    }

    @Transactional
    public TokenPair register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalStateException("Пользователь с таким email уже существует");
        }
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));

        var clientRole = roleRepository.findByName("CLIENT").orElseThrow(()->new EntityNotFoundException("Роль клиент не была найдена"));
        user.setRole(clientRole);

        userRepository.save(user);

        return generateTokenPair(user.getId(), user.getEmail(), "CLIENT", null);
    }

    private TokenPair generateTokenPair(UUID userId, String email, String role, Long stationId) {
        String access = jwtService.createAccessToken(userId,email, role, stationId);
        String refresh = jwtService.createRefreshToken(email);
        return new TokenPair(access, refresh);
    }
}
