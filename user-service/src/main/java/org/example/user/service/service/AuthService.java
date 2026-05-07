package org.example.user.service.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.user.service.dto.request.LoginRequest;
import org.example.user.service.dto.request.RegisterRequest;
import org.example.user.service.dto.response.TokenPair;
import org.example.user.service.entity.User;
import org.example.user.service.repository.RoleRepository;
import org.example.user.service.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;

    @Transactional
    public TokenPair login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email());

        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Неверный email или пароль");
        }

        Long stationId = user.getWorkplaceId();

        return generateTokenPair(user.getId(),user.getEmail(), user.getRole().getName(), stationId);
    }

    @Transactional
    @CacheEvict(cacheNames = "users", allEntries = true)
    public TokenPair register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()) != null) {
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

    private TokenPair generateTokenPair(Long userId,String email, String role, Long stationId) {
        String access = jwtService.createAccessToken(userId,email, role, stationId);
        String refresh = jwtService.createRefreshToken(email);
        return new TokenPair(access, refresh);
    }
}
