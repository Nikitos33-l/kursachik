package org.example.user.service.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.user.service.dto.request.LoginRequest;
import org.example.user.service.dto.request.RegisterRequest;
import org.example.user.service.dto.response.TokenPair;
import org.example.user.service.entity.*;
import org.example.user.service.repository.RoleRepository;
import org.example.user.service.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID; // Не забываем импорт

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private final UUID userId = UUID.randomUUID();

    @Test
    @DisplayName("Успешный логин")
    void login_Success() {
        LoginRequest request = new LoginRequest("test@mail.com", "password");
        Role role = new Role();
        role.setName("CLIENT");

        User user = User.builder().id(userId).email("test@mail.com")
                .password("encoded_password").role(role).workplaceId(10L).build();

        when(userRepository.findByEmail(request.email())).thenReturn(user);
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(true);

        when(jwtService.createAccessToken(any(UUID.class), anyString(), anyString(), anyLong())).thenReturn("access");
        when(jwtService.createRefreshToken(anyString())).thenReturn("refresh");

        TokenPair result = authService.login(request);

        assertNotNull(result);
        assertEquals("access", result.accessToken());
        assertEquals("refresh", result.refreshToken());

        verify(jwtService).createAccessToken(userId, "test@mail.com", "CLIENT", 10L);
    }

    @Test
    @DisplayName("Логин провален: пользователь не найден")
    void login_UserNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(null);

        assertThrows(BadCredentialsException.class, () -> authService.login(new LoginRequest("a", "b")));
    }

    @Test
    @DisplayName("Логин провален: неверный пароль")
    void login_WrongPassword() {
        User user = new User();
        user.setPassword("correct");
        when(userRepository.findByEmail(anyString())).thenReturn(user);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(new LoginRequest("a", "b")));
    }

    @Test
    @DisplayName("Успешная регистрация")
    void register_Success() {
        RegisterRequest request = new RegisterRequest("New User", "new@mail.com", "pass");
        Role clientRole = new Role();
        clientRole.setName("CLIENT");

        when(userRepository.findByEmail(request.email())).thenReturn(null);
        when(roleRepository.findByName("CLIENT")).thenReturn(Optional.of(clientRole));
        when(passwordEncoder.encode(request.password())).thenReturn("encoded_pass");

        when(jwtService.createAccessToken(any(), anyString(), anyString(), any())).thenReturn("access");
        when(jwtService.createRefreshToken(anyString())).thenReturn("refresh");

        TokenPair result = authService.register(request);

        assertNotNull(result);
        verify(userRepository).save(any(User.class));
        verify(jwtService).createAccessToken(any(), eq("new@mail.com"), eq("CLIENT"), isNull());
    }

    @Test
    @DisplayName("Регистрация провалена: email занят")
    void register_EmailExists() {
        when(userRepository.findByEmail(anyString())).thenReturn(new User());

        assertThrows(IllegalStateException.class, () -> authService.register(new RegisterRequest("n", "e", "p")));
    }

    @Test
    @DisplayName("Регистрация провалена: роль CLIENT не найдена")
    void register_RoleNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(null);
        when(roleRepository.findByName("CLIENT")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> authService.register(new RegisterRequest("n", "e", "p")));
    }
}