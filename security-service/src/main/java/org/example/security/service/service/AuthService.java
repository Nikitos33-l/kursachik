package org.example.security.service.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.security.service.dto.request.LoginRequest;
import org.example.security.service.dto.request.RegisterRequest;
import org.example.security.service.dto.response.TokenPair;
import org.example.security.service.entity.User;
import org.example.security.service.producer.UserEventProducer;
import org.example.security.service.repository.RoleRepository;
import org.example.security.service.repository.UserRepository;
import org.example.user.contracts.UserRegisterEvent;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final UserEventProducer userEventProducer;

    @Transactional
    public TokenPair login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email()).orElseThrow(()->new BadCredentialsException("Неверный email или пароль"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
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
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));

        user.setId(UUID.randomUUID());

        var clientRole = roleRepository.findByName("CLIENT").orElseThrow(()->new EntityNotFoundException("Роль клиент не была найдена"));
        user.setRole(clientRole);

        UserRegisterEvent userRegisterEvent = new UserRegisterEvent(user.getId(), user.getEmail(), request.name(), user.getPassword(), clientRole.getName(), null);
        runAfterCommit(()->userEventProducer.publishUserRegisterEvent(userRegisterEvent));

        userRepository.save(user);

        return generateTokenPair(user.getId(), user.getEmail(), "CLIENT", null);
    }

    private static void runAfterCommit(Runnable runnable) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                runnable.run();
            }
        });
    }

    private TokenPair generateTokenPair(UUID userId, String email, String role, Long stationId) {
        String access = jwtService.createAccessToken(userId,email, role, stationId);
        String refresh = jwtService.createRefreshToken(email);
        return new TokenPair(access, refresh);
    }
}
