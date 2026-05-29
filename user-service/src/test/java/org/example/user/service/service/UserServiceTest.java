package org.example.user.service.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.securitycommon.UserPrincipal;
import org.example.user.api.requestDto.OrderUserMappingRequest;
import org.example.user.api.responceDto.OrderInfoFromUserServiceDto;
import org.example.user.api.responceDto.UserDto;
import org.example.user.api.responceDto.ValidationResponse;
import org.example.user.contracts.UserRegisterEvent;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private VehicleMapper vehicleMapper;
    @Mock private CarService carService;
    @Mock private UserEventProducer userEventProducer;
    @Mock private CacheManager cacheManager;
    @Mock private Cache cache;

    @InjectMocks
    private UserService userService;

    private final UUID userId = UUID.randomUUID();
    private final UUID clientId = UUID.randomUUID();
    private final UUID workerId = UUID.randomUUID();
    private User sampleUser;
    private Role workerRole;

    @BeforeEach
    void setUp() {
        workerRole = new Role();
        workerRole.setName("WORKER");

        sampleUser = User.builder()
                .id(userId)
                .name("Ivan")
                .email("ivan@gmail.com")
                .role(workerRole)
                .workplaceId(1L)
                .build();
    }

    @Test
    @DisplayName("Успешное получение всех пользователей станции")
    void getAll_Success() {
        Long stationId = 1L;
        when(userRepository.findAll(stationId)).thenReturn(Collections.emptyList());
        when(userMapper.toListResponseUserDto(any())).thenReturn(Collections.emptyList());

        List<ResponseUserDto> result = userService.getAll(stationId);

        verify(userRepository).findAll(stationId);
        assertNotNull(result);
    }

    @Test
    @DisplayName("Успешное получение всех работников станции")
    void getAllWorkers_Success() {
        Long stationId = 1L;
        UserShortResponse response = new UserShortResponse(workerId, "Ivan", "ivan@gmail.com");

        when(userRepository.findAllByRole_NameAndWorkplaceId("WORKER", stationId))
                .thenReturn(List.of(sampleUser));
        when(userMapper.toListShortResponse(any())).thenReturn(List.of(response));

        List<UserShortResponse> result = userService.getAllWorkers(stationId);

        assertEquals(1, result.size());
        assertEquals("Ivan", result.get(0).name());
    }

    @Test
    @DisplayName("Успешное получение краткой информации о пользователе")
    void getInfo_Success() {
        UserShortResponse expectedResponse = new UserShortResponse(userId, "Ivan", "ivan@gmail.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userMapper.toShortResponse(sampleUser)).thenReturn(expectedResponse);

        UserShortResponse result = userService.getInfo(userId);

        assertNotNull(result);
        assertEquals(expectedResponse.name(), result.name());
    }

    @Test
    @DisplayName("Успешное удаление пользователя с очисткой кэша")
    void deleteUser_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(cacheManager.getCache(anyString())).thenReturn(cache);

        try (MockedStatic<TransactionSynchronizationManager> tsmMock = mockStatic(TransactionSynchronizationManager.class)) {
            tsmMock.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);

            userService.deleteUser(userId);

            verify(userRepository, times(1)).delete(sampleUser);
            verify(cache, times(2)).evict(1L); // Сбросит USERS_CACHE и WORKERS_CACHE
            tsmMock.verify(() -> TransactionSynchronizationManager.registerSynchronization(any()), times(1));
        }
    }

    @Test
    @DisplayName("Успешное обновление данных пользователя")
    void updateUser_Success() {
        RequestUpdateUserDto updateDto = new RequestUpdateUserDto("New Name", "new@mail.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.existsByEmail("new@mail.com")).thenReturn(false);
        when(cacheManager.getCache(anyString())).thenReturn(cache);

        try (MockedStatic<TransactionSynchronizationManager> tsmMock = mockStatic(TransactionSynchronizationManager.class)) {
            tsmMock.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);

            userService.updateUser(userId, updateDto);

            assertEquals("new@mail.com", sampleUser.getEmail());
            assertEquals("New Name", sampleUser.getName());
            verify(cache, times(2)).evict(1L);
        }
    }

    @Test
    @DisplayName("Ошибка обновления пользователя, если email уже занят")
    void updateUser_EmailAlreadyExists() {
        RequestUpdateUserDto updateDto = new RequestUpdateUserDto("New Name", "occupied@mail.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.existsByEmail("occupied@mail.com")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> userService.updateUser(userId, updateDto));
    }

    @Test
    @DisplayName("Ошибка, если пользователь для обновления не найден")
    void updateUser_UserNotFound() {
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> userService.updateUser(userId, new RequestUpdateUserDto("Name", "email@mail.com")));
    }

    @Test
    @DisplayName("Сбор информации для нескольких заказов")
    void getInfoForOrders_Success() {
        Long orderId = 100L;
        Long vehicleId = 50L;
        Set<UUID> workerIds = Set.of(workerId);

        OrderUserMappingRequest request = new OrderUserMappingRequest(orderId, clientId, workerIds, vehicleId);

        User clientUser = User.builder().id(clientId).name("Client").build();
        User workerUser = User.builder().id(workerId).name("Worker").build();
        Vehicle vehicle = new Vehicle();

        UserDto userDto = new UserDto(clientId, "Name", "Email");

        when(userRepository.findAllByIdIn(anySet())).thenReturn(List.of(clientUser, workerUser));
        when(carService.getVehiclesMap(anySet())).thenReturn(Map.of(vehicleId, vehicle));
        when(userMapper.toDto(any(User.class))).thenReturn(userDto);

        Map<Long, OrderInfoFromUserServiceDto> result = userService.getInfoForOrders(List.of(request));

        assertNotNull(result);
        assertTrue(result.containsKey(orderId));
        assertEquals(userDto, result.get(orderId).getClient());
        verify(carService, times(1)).getVehiclesMap(anySet());
    }

    @Test
    @DisplayName("Получение информации для одного конкретного заказа")
    void getInfoForOrder_Success() {
        OrderUserMappingRequest request = new OrderUserMappingRequest(1L, clientId, Set.of(workerId), 50L);
        Vehicle vehicle = new Vehicle();
        User client = User.builder().id(clientId).build();

        when(vehicleRepository.findById(50L)).thenReturn(Optional.of(vehicle));
        when(userRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(userRepository.findAllByIdIn(Set.of(workerId))).thenReturn(List.of(sampleUser));

        userService.getInfoForOrder(request);

        verify(vehicleMapper).toDto(vehicle);
        verify(userMapper).toDto(client);
        verify(userMapper).toDtoList(anyList());
    }

    @Test
    @DisplayName("Валидация списка работников - Успех")
    void validateWorkers_Success() {
        Set<UUID> ids = Set.of(userId);
        when(userRepository.findAllByIdIn(ids)).thenReturn(List.of(sampleUser));

        ValidationResponse response = userService.validateWorkers(ids);

        assertTrue(response.exists());
        assertNotNull(response.emails());
        assertEquals("ivan@gmail.com", response.emails().get(userId));
    }

    @Test
    @DisplayName("Валидация списка работников - ошибка, если найдены не все")
    void validateWorkers_Fail() {
        Set<UUID> ids = Set.of(UUID.randomUUID(), UUID.randomUUID());
        when(userRepository.findAllByIdIn(ids)).thenReturn(List.of(sampleUser));

        ValidationResponse response = userService.validateWorkers(ids);

        assertFalse(response.exists());
        assertNull(response.emails());
    }

    @Test
    @DisplayName("Добавление пользователя: Супер-админ может указать любую станцию")
    void addUser_SuperAdmin_SetsStationFromDto() {
        RequestAddUserDto dto = new RequestAddUserDto("Admin", "admin@st.com", "pass", "ADMIN", 99L);
        UserPrincipal principal = mock(UserPrincipal.class);
        var authority = new SimpleGrantedAuthority("ROLE_SUPERADMIN");

        doReturn(List.of(authority)).when(principal).authorities();
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName(anyString())).thenReturn(Optional.of(workerRole));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(cacheManager.getCache(anyString())).thenReturn(cache);

        try (MockedStatic<TransactionSynchronizationManager> tsmMock = mockStatic(TransactionSynchronizationManager.class)) {
            tsmMock.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);

            userService.addUser(dto, principal);

            verify(userRepository).save(argThat(user -> user.getWorkplaceId().equals(99L)));
        }
    }

    @Test
    @DisplayName("Добавление пользователя: Обычный админ привязан к своей станции")
    void addUser_RegularAdmin_SetsStationFromPrincipal() {
        RequestAddUserDto dto = new RequestAddUserDto("Worker", "w@st.com", "pass", "WORKER", 99L);
        UserPrincipal principal = mock(UserPrincipal.class);

        when(principal.authorities()).thenReturn(Collections.emptyList());
        when(principal.stationId()).thenReturn(10L);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName(anyString())).thenReturn(Optional.of(workerRole));
        when(cacheManager.getCache(anyString())).thenReturn(cache);

        try (MockedStatic<TransactionSynchronizationManager> tsmMock = mockStatic(TransactionSynchronizationManager.class)) {
            tsmMock.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);

            userService.addUser(dto, principal);

            verify(userRepository).save(argThat(user -> user.getWorkplaceId().equals(10L)));
        }
    }

    @Test
    @DisplayName("Успешная обработка внешней регистрации пользователя (Event Consumer)")
    void handleUserRegister_Success() {
        UserRegisterEvent event = new UserRegisterEvent(userId, "Reg", "reg@mail.com", "hash", "WORKER", 1L);
        when(userRepository.existsByEmail(event.email())).thenReturn(false);
        when(roleRepository.findByName("WORKER")).thenReturn(Optional.of(workerRole));

        userService.handleUserRegister(event);

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Успешное каскадное удаление пользователей по ID станции")
    void deleteByWorkplace_Success() {
        Long stationId = 1L;

        userService.deleteByWorkplace(stationId);

        verify(userRepository, times(1)).deleteAllByWorkplaceId(stationId);
    }
}