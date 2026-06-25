package org.example.security.service.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.security.service.dto.request.LoginRequest;
import org.example.security.service.dto.request.RegisterRequest;
import org.example.security.service.dto.response.TokenPair;
import org.example.security.service.entity.Role;
import org.example.security.service.entity.User;
import org.example.security.service.repository.RoleRepository;
import org.example.security.service.repository.UserRepository;
import org.example.user.contracts.UserRegisterEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RoleRepository roleRepository;
    @Mock private JwtService jwtService;
    @Mock private UserOutboxService userOutboxService;

    @InjectMocks
    private AuthService authService;

    private final UUID userId = UUID.randomUUID();
    private User sampleUser;
    private Role clientRole;

    @BeforeEach
    void setUp() {
        clientRole = new Role();
        clientRole.setName("CLIENT");

        sampleUser = User.builder()
                .id(userId)
                .email("test@mail.com")
                .password("encoded_password")
                .role(clientRole)
                .workplaceId(10L)
                .build();
    }

    @Test
    @DisplayName("Успешный логин")
    void login_Success() {
        LoginRequest request = new LoginRequest("test@mail.com", "password");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches(request.password(), sampleUser.getPassword())).thenReturn(true);

        when(jwtService.createAccessToken(eq(userId), eq("test@mail.com"), eq("CLIENT"), eq(10L))).thenReturn("access");
        when(jwtService.createRefreshToken(eq("test@mail.com"))).thenReturn("refresh");

        TokenPair result = authService.login(request);

        assertNotNull(result);
        assertEquals("access", result.accessToken());
        assertEquals("refresh", result.refreshToken());

        verify(jwtService).createAccessToken(userId, "test@mail.com", "CLIENT", 10L);
    }

    @Test
    @DisplayName("Логин провален: пользователь не найден")
    void login_UserNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> authService.login(new LoginRequest("a", "b")));
    }

    @Test
    @DisplayName("Логин провален: неверный пароль")
    void login_WrongPassword() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(new LoginRequest("a", "b")));
    }

    @Test
    @DisplayName("Успешная регистрация и сохранение события в Outbox")
    void register_Success() {
        RegisterRequest request = new RegisterRequest("New User", "new@mail.com", "pass");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.password())).thenReturn("encoded_pass");
        when(roleRepository.findByName("CLIENT")).thenReturn(Optional.of(clientRole));

        when(jwtService.createAccessToken(any(UUID.class), eq("new@mail.com"), eq("CLIENT"), isNull())).thenReturn("access");
        when(jwtService.createRefreshToken(anyString())).thenReturn("refresh");

        TokenPair result = authService.register(request);

        assertNotNull(result);
        assertEquals("access", result.accessToken());

        verify(userRepository).save(any(User.class));
        verify(userOutboxService).saveRegisterEvent(any(UserRegisterEvent.class));
    }

    @Test
    @DisplayName("Регистрация провалена: email занят")
    void register_EmailExists() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(sampleUser));

        assertThrows(IllegalStateException.class, () -> authService.register(new RegisterRequest("n", "e", "p")));
    }

    @Test
    @DisplayName("Регистрация провалена: роль CLIENT не найдена")
    void register_RoleNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_pass");
        when(roleRepository.findByName("CLIENT")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> authService.register(new RegisterRequest("n", "e", "p")));
    }
}