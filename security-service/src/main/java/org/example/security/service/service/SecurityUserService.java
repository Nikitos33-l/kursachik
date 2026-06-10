package org.example.security.service.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.security.service.entity.Role;
import org.example.security.service.entity.User;
import org.example.security.service.repository.RoleRepository;
import org.example.security.service.repository.UserRepository;
import org.example.user.contracts.UserCreatedEvent;
import org.example.user.contracts.UserUpdateEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BlackListService blackListService;

    @Transactional
    public void deleteUser(UUID id) {
        log.info("Синхронизация: Удаление пользователя с UUID: {}", id);
        if (!userRepository.existsById(id)) {
            log.warn("Синхронизация: Попытка удаления, но пользователь UUID {} не найден в локальной БД security", id);
            return;
        }
        userRepository.deleteById(id);
        blackListService.blacklistUser(id.toString());
        log.info("Синхронизация: Пользователь UUID: {} удален. Активные сессии заблокированы", id);
    }

    @Transactional
    public void updateUser(UserUpdateEvent event) {
        log.info("Синхронизация: Обновление профиля пользователя UUID: {}", event.id());

        User user = userRepository.findById(event.id())
                .orElseThrow(() -> {
                    log.error("Синхронизация КРИТИЧЕСКАЯ ОШИБКА: Невозможно обновить данные, UUID {} отсутствует в локальной БД", event.id());
                    return new EntityNotFoundException("Пользователь с таким id не был найден");
                });

        String oldEmail = user.getEmail();
        user.setEmail(event.email());

        blackListService.blacklistUser(event.id().toString());
        log.info("Синхронизация: Профиль UUID {} изменен (Старый email: '{}', Новый email: '{}'). Старые сессии аннулированы",
                event.id(), oldEmail, event.email());
    }

    @Transactional
    public void createUser(UserCreatedEvent event) {
        log.info("Синхронизация: Создание новой локальной записи пользователя. UUID: {}, Email: {}, Роль: {}",
                event.id(), event.email(), event.role());

        if (userRepository.existsById(event.id())) {
            log.warn("Синхронизация: Запись для UUID {} уже существует. Перезапись данных", event.id());
        }

        Role role = roleRepository.findByName(event.role())
                .orElseThrow(() -> {
                    log.error("Синхронизация КРИТИЧЕСКАЯ ОШИБКА: Роль '{}' не поддерживается подсистемой авторизации", event.role());
                    return new EntityNotFoundException("Роль с таким названием не была найдена");
                });

        User user = User.builder()
                .id(event.id())
                .email(event.email())
                .role(role)
                .password(event.password())
                .workplaceId(event.workplaceId())
                .build();

        userRepository.save(user);
        log.info("Синхронизация: Локальная копия пользователя для UUID {} успешно добавлена", event.id());
    }
}