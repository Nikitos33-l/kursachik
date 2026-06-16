package org.example.security.service.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.security.service.entity.Role;
import org.example.security.service.entity.User;
import org.example.security.service.repository.RoleRepository;
import org.example.security.service.repository.UserRepository;
import org.example.user.contracts.UserCreatedEvent;
import org.example.user.contracts.UserUpdateEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private BlackListService blackListService;

    @InjectMocks
    private SecurityUserService securityUserService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Test
    @DisplayName("Должен удалить пользователя по ID и добавить его в блэклист")
    void shouldDeleteUserAndAddToBlacklist() {
        UUID userId = UUID.randomUUID();

        when(userRepository.existsById(userId)).thenReturn(true);

        securityUserService.deleteUser(userId);

        verify(userRepository, Mockito.times(1)).existsById(userId);
        verify(userRepository, Mockito.times(1)).deleteById(userId);
        verify(blackListService, Mockito.times(1)).blacklistUser(userId.toString());
    }

    @Test
    @DisplayName("Должен обновить email пользователя и добавить его в блэклист, если пользователь существует")
    void shouldUpdateUserEmailAndAddToBlacklist() {
        UUID userId = UUID.randomUUID();
        UserUpdateEvent event = new UserUpdateEvent(userId, "new.email@example.com");

        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setEmail("old.email@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        securityUserService.updateUser(event);

        assertThat(existingUser.getEmail()).isEqualTo("new.email@example.com");

        verify(userRepository, times(1)).findById(userId);
        verify(blackListService, times(1)).blacklistUser(userId.toString());
    }

    @Test
    @DisplayName("Должен выбросить EntityNotFoundException при попытке обновить несуществующего пользователя")
    void shouldThrowExceptionWhenUpdatingNonExistentUser() {
        UUID userId = UUID.randomUUID();
        UserUpdateEvent event = new UserUpdateEvent(userId, "new.email@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> securityUserService.updateUser(event))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Пользователь с таким id не был найден");

        verify(blackListService, never()).blacklistUser(anyString());
    }

    @Test
    @DisplayName("Должен создать и сохранить пользователя, если указанная роль существует")
    void shouldCreateUserWhenRoleExists() {
        UUID userId = UUID.randomUUID();
        UserCreatedEvent event = new UserCreatedEvent(
                userId,
                "test@example.com",
                "password123",
                "CLIENT",
                1L
        );

        Role role = new Role(1L, "CLIENT");
        when(roleRepository.findByName("CLIENT")).thenReturn(Optional.of(role));

        securityUserService.createUser(event);

        verify(roleRepository, times(1)).findByName("CLIENT");

        verify(userRepository, times(1)).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getId()).isEqualTo(userId);
        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
        assertThat(savedUser.getPassword()).isEqualTo("password123");
        assertThat(savedUser.getRole()).isEqualTo(role);
        assertThat(savedUser.getWorkplaceId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Должен выбросить EntityNotFoundException при попытке создать пользователя с несуществующей ролью")
    void shouldThrowExceptionWhenCreatingUserWithNonExistentRole() {
        UserCreatedEvent event = new UserCreatedEvent(
                UUID.randomUUID(),
                "test@example.com",
                "password123",
                "UNKNOWN_ROLE",
                1L
        );

        when(roleRepository.findByName("UNKNOWN_ROLE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> securityUserService.createUser(event))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Роль с таким названием не была найдена");

        verify(userRepository, never()).save(any(User.class));
    }
}