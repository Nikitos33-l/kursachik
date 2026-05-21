package org.example.user.service.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.user.contracts.UserCreatedEvent;
import org.example.user.contracts.UserUpdateEvent;
import org.example.user.service.publisher.UserEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.order.service.api.common.client.OrderServiceClient;
import org.example.securitycommon.UserPrincipal;
import org.example.user.api.requestDto.OrderUserMappingRequest;
import org.example.user.api.responceDto.OrderInfoFromUserServiceDto;
import org.example.user.api.responceDto.UserDto;
import org.example.user.api.responceDto.ValidationResponse;
import org.example.user.service.dto.request.RequestAddUserDto;
import org.example.user.service.dto.request.RequestUpdateUserDto;
import org.example.user.service.dto.response.ResponseUserDto;
import org.example.user.service.dto.response.UserShortResponse;
import org.example.user.service.entity.Role;
import org.example.user.service.entity.User;
import org.example.user.service.entity.Vehicle;
import org.example.user.service.mapper.UserMapper;
import org.example.user.service.mapper.VehicleMapper;
import org.example.user.service.repository.RoleRepository;
import org.example.user.service.repository.UserRepository;
import org.example.user.service.repository.VehicleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final OrderServiceClient orderServiceClient;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;
    private final CarService carService;
    private final UserEventPublisher userEventPublisher;

    @Transactional(readOnly = true)
    public List<ResponseUserDto> getAll(Long stationId) {
        return userMapper.toListResponseUserDto(userRepository.findAll(stationId));
    }

    @Transactional(readOnly = true)
    public List<UserShortResponse> getAllWorkers(Long stationId) {
        return userMapper.toListShortResponse(userRepository.
                findAllByRole_NameAndWorkplaceId("WORKER", stationId));
    }

    @Transactional(readOnly = true)
    public UserShortResponse getInfo(UUID id) {
        return userMapper.toShortResponse(getById(id));
    }

    @Transactional(readOnly = true)
    public Map<Long, OrderInfoFromUserServiceDto> getInfoForOrders(List<OrderUserMappingRequest> request) {
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
                .orElseThrow(() -> new EntityNotFoundException("Автомобиля с таким id не существует"));
    }

    @Transactional(readOnly = true)
    public ValidationResponse validateWorkers(Set<UUID> ids) {
        List<User> workers = userRepository.findAllByIdIn(ids);
        if (workers.size() != ids.size()) {
            return new ValidationResponse(false, null);
        }

        Map<UUID, String> emails = workers.stream().collect(Collectors.toMap(
                User::getId, User::getEmail
        ));

        return new ValidationResponse(true, emails);
    }

    @Transactional
    public void deleteUser(UUID id) {
        userRepository.deleteById(id);
        runAfterCommit(()->userEventPublisher.publishUserDeletedEvents(id));
    }

    @Transactional
    public void updateUser(UUID id, RequestUpdateUserDto userDto) {
        User user = getById(id);
        if(!user.getEmail().equals(userDto.email())){
            checkUserExists(userDto.email());
            UserUpdateEvent message = new UserUpdateEvent(id, userDto.email().trim());
            runAfterCommit(()->userEventPublisher.publishUserUpdateEvents(message));
        }
        user.setEmail(userDto.email().trim());
        user.setName(userDto.name().trim());
    }

    private User getById(UUID id) {
        return userRepository.
                findById(id).orElseThrow(() -> new EntityNotFoundException("Нету пользователя с таким id"));
    }


    @Transactional
    public void addUser(RequestAddUserDto userDto, UserPrincipal userPrincipal) {
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

        UserCreatedEvent message= new UserCreatedEvent(user.getId()
                ,user.getEmail(),user.getPassword(),user.getRole().getName(),user.getWorkplaceId());

        userRepository.save(user);

        runAfterCommit(()->userEventPublisher.publishUserCreatedEvents(message));
    }

    private void checkUserExists(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException("Пользователь с таким email уже существует");
        }
    }

    private Role getRoleByName(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new EntityNotFoundException("Роли с таким именем не существует"));
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
    public void deleteByWorkplace(Long id) {
        userRepository.deleteAllByWorkplaceId(id);
    }

    public static void runAfterCommit(Runnable runnable) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                runnable.run();
            }
        });
    }
}