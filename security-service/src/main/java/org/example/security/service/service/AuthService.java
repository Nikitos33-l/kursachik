package org.example.security.service.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.security.service.dto.request.LoginRequest;
import org.example.security.service.dto.request.RegisterRequest;
import org.example.security.service.dto.response.TokenPair;
import org.example.security.service.entity.User;
import org.example.security.service.repository.RoleRepository;
import org.example.security.service.repository.UserRepository;
import org.example.user.contracts.UserRegisterEvent;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final UserOutboxService userOutboxService;

    @Transactional
    public TokenPair login(LoginRequest request) {
        log.info("Процесс аутентификации для email: {}", request.email());

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("Аутентификация провалена: пользователь с email '{}' не зарегистрирован", request.email());
                    return new BadCredentialsException("Неверный email или пароль");
                });

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            log.warn("Аутентификация провалена: неверный пароль для пользователя '{}'", request.email());
            throw new BadCredentialsException("Неверный email или пароль");
        }

        Long stationId = user.getWorkplaceId();
        log.info("Пользователь '{}' успешно аутентифицирован. ID: {}, Роль: {}, СТО ID: {}",
                user.getEmail(), user.getId(), user.getRole().getName(), stationId);

        return generateTokenPair(user.getId(), user.getEmail(), user.getRole().getName(), stationId);
    }

    @Transactional
    public TokenPair register(RegisterRequest request) {
        log.info("Запуск регистрации нового клиента с email: {}", request.email());

        if (userRepository.findByEmail(request.email()).isPresent()) {
            log.warn("Регистрация прервана: email '{}' уже занят", request.email());
            throw new IllegalStateException("Пользователь с таким email уже существует");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setId(UUID.randomUUID());

        var clientRole = roleRepository.findByName("CLIENT")
                .orElseThrow(() -> {
                    log.error("Критическая ошибка: Дефолтная роль 'CLIENT' не найдена в базе данных!");
                    return new EntityNotFoundException("Роль клиент не была найдена");
                });
        user.setRole(clientRole);


        userRepository.save(user);
        log.info("Профиль пользователя (UUID: {}) успешно сохранен в БД со статусом CLIENT", user.getId());

        UserRegisterEvent event = new UserRegisterEvent(user.getId(),user.getEmail(),request.name(), user.getPassword(), clientRole.getName(),user.getWorkplaceId());

        userOutboxService.saveRegisterEvent(event);

        return generateTokenPair(user.getId(), user.getEmail(), "CLIENT", null);
    }


    private TokenPair generateTokenPair(UUID userId, String email, String role, Long stationId) {
        log.debug("Генерация новой пары токенов для {}", email);
        String access = jwtService.createAccessToken(userId, email, role, stationId);
        String refresh = jwtService.createRefreshToken(email);
        return new TokenPair(access, refresh);
    }
}