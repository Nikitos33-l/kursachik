package org.example.user.service.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Добавили импорт логгера
import org.example.securitycommon.UserPrincipal;
import org.example.user.api.requestDto.OrderUserMappingRequest;
import org.example.user.api.responceDto.OrderInfoFromUserServiceDto;
import org.example.user.api.responceDto.UserDto;
import org.example.user.api.responceDto.ValidationResponse;
import org.example.user.contracts.UserCreatedEvent;
import org.example.user.contracts.UserRegisterEvent;
import org.example.user.contracts.UserUpdateEvent;
import org.example.user.service.constant.CacheNames;
import org.example.user.service.dto.request.RequestAddUserDto;
import org.example.user.service.dto.request.RequestUpdateUserDto;
import org.example.user.service.dto.response.ResponseUserDto;
import org.example.user.service.dto.response.UserShortResponse;
import org.example.user.service.entity.Role;
import org.example.user.service.entity.User;
import org.example.user.service.entity.Vehicle;
import org.example.user.service.mapper.UserMapper;
import org.example.user.service.mapper.VehicleMapper;
import org.example.user.service.producer.UserEventProducer;
import org.example.user.service.repository.RoleRepository;
import org.example.user.service.repository.UserRepository;
import org.example.user.service.repository.VehicleRepository;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;
    private final CarService carService;
    private final UserEventProducer userEventProducer;
    private final CacheManager cacheManager;

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.USERS_CACHE, key = "#stationId")
    public List<ResponseUserDto> getAll(Long stationId) {
        log.debug("Запрос списка всех пользователей для СТО ID: {}", stationId);
        return userMapper.toListResponseUserDto(userRepository.findAll(stationId));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.WORKERS_CACHE, key = "#stationId")
    public List<UserShortResponse> getAllWorkers(Long stationId) {
        log.debug("Запрос списка всех сотрудников для СТО ID: {}", stationId);
        return userMapper.toListShortResponse(userRepository.findAllByRole_NameAndWorkplaceId("WORKER", stationId));
    }

    @Transactional(readOnly = true)
    public UserShortResponse getInfo(UUID id) {
        log.debug("Получение краткой информации о пользователе ID: {}", id);
        return userMapper.toShortResponse(getById(id));
    }

    @Transactional(readOnly = true)
    public Map<Long, OrderInfoFromUserServiceDto> getInfoForOrders(List<OrderUserMappingRequest> request) {
        log.debug("Сбор пакетных данных пользователей для {} заказов", request.size());

        Set<UUID> workerIds = request.stream()
                .flatMap(r -> r.workersId().stream()).collect(Collectors.toSet());

        Set<UUID> clientIds = request.stream()
                .map(OrderUserMappingRequest::userId).collect(Collectors.toSet());

        Set<Long> vehiclesIds = request.stream()
                .map(OrderUserMappingRequest::vehicleId).collect(Collectors.toSet());

        Map<UUID, User> workers = getUsersMap(workerIds);
        Map<UUID, User> clients = getUsersMap(clientIds);
        Map<Long, Vehicle> vehicles = carService.getVehiclesMap(vehiclesIds);

        return request.stream().collect(Collectors.toMap(
                OrderUserMappingRequest::orderId, r ->
                        new OrderInfoFromUserServiceDto(
                                getClientOfOrder(r.userId(), clients),
                                getWorkersOfOrder(r.workersId(), workers),
                                carService.getVehicleOfOrder(r.vehicleId(), vehicles)
                        )
        ));
    }

    private Map<UUID, User> getUsersMap(Set<UUID> ids) {
        return userRepository.findAllByIdIn(ids).stream().collect(Collectors.toMap(
                User::getId, w -> w
        ));
    }

    private UserDto getClientOfOrder(UUID clientId, Map<UUID, User> clients) {
        return userMapper.toDto(clients.get(clientId));
    }

    private List<UserDto> getWorkersOfOrder(Set<UUID> workerIds, Map<UUID, User> workers) {
        return workerIds.stream().map(id -> userMapper.toDto(workers.get(id))).toList();
    }

    @Transactional(readOnly = true)
    public OrderInfoFromUserServiceDto getInfoForOrder(OrderUserMappingRequest request) {
        log.debug("Сбор данных пользователей для одиночного заказа ID: {}", request.orderId());
        Vehicle vehicle = getVehicleById(request.vehicleId());
        User client = getById(request.userId());
        List<User> workers = userRepository.findAllByIdIn(request.workersId());

        return OrderInfoFromUserServiceDto.builder().client(userMapper.toDto(client))
                .vehicle(vehicleMapper.toDto(vehicle))
                .workers(userMapper.toDtoList(workers))
                .build();
    }

    private Vehicle getVehicleById(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Ошибка поиска автомобиля: ID {} не найден", id);
                    return new EntityNotFoundException("Автомобиля с таким id не существует");
                });
    }

    @Transactional(readOnly = true)
    public ValidationResponse validateWorkers(Set<UUID> ids) {
        log.debug("Валидация списка сотрудников. Передано ID на проверку: {}", ids.size());
        List<User> workers = userRepository.findAllByIdIn(ids);
        if (workers.size() != ids.size()) {
            log.warn("Валидация сотрудников провалена. Найдено {} из {} запрошенных", workers.size(), ids.size());
            return new ValidationResponse(false, null);
        }

        Map<UUID, String> emails = workers.stream().collect(Collectors.toMap(
                User::getId, User::getEmail
        ));

        return new ValidationResponse(true, emails);
    }

    @Transactional(readOnly = true)
    public String getEmailById(UUID id) {
        return getById(id).getEmail();
    }

    @Transactional
    public void deleteUser(UUID id) {
        log.info("Запуск удаления пользователя ID: {}", id);
        User user = getById(id);
        Long stationId = user.getWorkplaceId();
        String roleName = user.getRole().getName();

        userRepository.delete(user);
        log.info("Пользователь ID: {} ({}) успешно удален из базы данных", id, roleName);

        evictStationCaches(stationId, roleName);

        runAfterCommit(() -> {
            log.info("Транзакция коммита удаления завершена. Отправка события UserDeletedEvent для ID: {}", id);
            userEventProducer.publishUserDeletedEvents(id);
        });
    }

    @Transactional
    public void updateUser(UUID id, RequestUpdateUserDto userDto) {
        log.info("Запуск обновления пользователя ID: {}", id);
        User user = getById(id);
        Long stationId = user.getWorkplaceId();
        String roleName = user.getRole().getName();

        if (!user.getEmail().equals(userDto.email())) {
            log.info("Пользователь ID: {} меняет email с '{}' на '{}'", id, user.getEmail(), userDto.email());
            checkUserExists(userDto.email());
            UserUpdateEvent message = new UserUpdateEvent(id, userDto.email().trim());

            runAfterCommit(() -> {
                log.info("Транзакция коммита обновления завершена. Отправка события UserUpdateEvent для ID: {}", id);
                userEventProducer.publishUserUpdateEvents(message);
            });
        }

        user.setEmail(userDto.email().trim());
        user.setName(userDto.name().trim());

        evictStationCaches(stationId, roleName);
        log.info("Данные пользователя ID: {} успешно сохранены в БД", id);
    }

    private User getById(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> {
            log.error("Пользователь с ID {} не найден в системе", id);
            return new EntityNotFoundException("Нету пользователя с таким id");
        });
    }

    @Transactional
    public void addUser(RequestAddUserDto userDto, UserPrincipal userPrincipal) {
        log.info("Запрос на добавление нового пользователя администратором. Email: {}, Роль: {}"
                , userDto.email(), userDto.role());

        checkUserExists(userDto.email());
        Role role = getRoleByName(userDto.role());
        Long stationId = getStationId(userDto, userPrincipal);

        User user = User.builder()
                .id(UUID.randomUUID())
                .name(userDto.name())
                .email(userDto.email())
                .role(role)
                .password(passwordEncoder.encode(userDto.password()))
                .workplaceId(stationId)
                .build();

        UserCreatedEvent message = new UserCreatedEvent(user.getId(), user.getEmail(),
                user.getPassword(), user.getRole().getName(), user.getWorkplaceId());

        userRepository.save(user);
        log.info("Новый пользователь успешно сохранен в БД с сгенерированным ID: {}", user.getId());

        evictStationCaches(stationId, role.getName());

        runAfterCommit(() -> {
            log.info("Транзакция коммита создания завершена. Отправка события UserCreatedEvent для ID: {}", user.getId());
            userEventProducer.publishUserCreatedEvents(message);
        });
    }

    @Transactional
    public void handleUserRegister(UserRegisterEvent event) {
        log.info("Получено внешнее событие регистрации UserRegisterEvent. ID: {}, Email: {}", event.id(), event.email());
        checkUserExists(event.email());
        Role role = getRoleByName(event.role());

        User user = User.builder().id(event.id())
                .name(event.name())
                .email(event.email())
                .role(role)
                .password(event.password())
                .workplaceId(event.workplaceId()).build();

        userRepository.save(user);
        log.info("Пользователь из внешнего события успешно зарегистрирован. ID: {}", user.getId());
    }

    private void checkUserExists(String email) {
        if (userRepository.existsByEmail(email)) {
            log.warn("Проверка уникальности провалена: пользователь с email '{}' уже существует", email);
            throw new IllegalStateException("Пользователь с таким email уже существует");
        }
    }

    private Role getRoleByName(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> {
                    log.error("Системная ошибка: Роль с именем '{}' отсутствует в БД", name);
                    return new EntityNotFoundException("Роли с таким именем не существует");
                });
    }

    private Long getStationId(RequestAddUserDto userDto, UserPrincipal userPrincipal) {
        if (isSuperAdmin(userPrincipal)) {
            return userDto.stationId();
        } else {
            return userPrincipal.stationId();
        }
    }

    private boolean isSuperAdmin(UserPrincipal user) {
        return user.authorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN"));
    }

    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = CacheNames.USERS_CACHE, key = "#id"),
                    @CacheEvict(value = CacheNames.WORKERS_CACHE, key = "#id")
            }
    )
    public void deleteByWorkplace(Long id) {
        log.warn("ВНИМАНИЕ: Запущено каскадное удаление всех пользователей для СТО ID: {}", id);
        userRepository.deleteAllByWorkplaceId(id);
        log.info("Все пользователи для СТО ID: {} успешно удалены", id);
    }

    private static void runAfterCommit(Runnable runnable) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                runnable.run();
            }
        });
    }

    private void evictStationCaches(Long stationId, String roleName) {
        log.debug("Сброс кэша пользователей (Название: {}) для станции ID: {}", CacheNames.USERS_CACHE, stationId);
        Cache usersCache = cacheManager.getCache(CacheNames.USERS_CACHE);
        if (usersCache != null) {
            usersCache.evict(stationId);
        }

        if ("WORKER".equals(roleName)) {
            log.debug("Удаленный/измененный пользователь являлся сотрудником. Сброс кэша (Название: {}) для станции ID: {}", CacheNames.WORKERS_CACHE, stationId);
            Cache workersCache = cacheManager.getCache(CacheNames.WORKERS_CACHE);
            if (workersCache != null) {
                workersCache.evict(stationId);
            }
        }
    }
}