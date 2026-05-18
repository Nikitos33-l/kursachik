package org.example.user.service.service;

import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
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
import org.example.user.service.repository.RoleRepository;
import org.example.user.service.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private OrderServiceClient orderServiceClient;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CarService carService;

    @InjectMocks
    private UserService userService;

    private final UUID userId = UUID.randomUUID();
    private final UUID clientId = UUID.randomUUID();
    private final UUID workerId = UUID.randomUUID();

    @Test
    @DisplayName("Успешное получение всех работников станции")
    void getAllWorkers_Success() {
        Long stationId = 1L;
        User worker = new User();
        worker.setId(workerId);
        worker.setName("Ivan");

        UserShortResponse response = new UserShortResponse(workerId, "Ivan", "ivan@gmail.com");

        when(userRepository.findAllByRole_NameAndWorkplaceId("WORKER", stationId))
                .thenReturn(List.of(worker));
        when(userMapper.toListShortResponse(any())).thenReturn(List.of(response));

        List<UserShortResponse> result = userService.getAllWorkers(stationId);

        assertEquals(1, result.size());
        assertEquals("Ivan", result.get(0).name());
    }

    @Test
    @DisplayName("Удаление пользователя и его заказов через Feign")
    void deleteUser_Success() {
        userService.deleteUser(userId);

        verify(orderServiceClient, times(1)).deleteByUser(userId);
        verify(userRepository, times(1)).deleteById(userId);
    }

    @Test
    @DisplayName("Ошибка при удалении пользователя, если Feign клиент упал")
    void deleteUser_FeignException() {
        doThrow(FeignException.class).when(orderServiceClient).deleteByUser(userId);

        assertThrows(RuntimeException.class, () -> userService.deleteUser(userId));
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
    @DisplayName("Успешное обновление данных пользователя")
    void updateUser_Success() {
        RequestUpdateUserDto updateDto = new RequestUpdateUserDto("New Name", "new@mail.com");
        User user = new User();
        user.setId(userId);
        user.setEmail("old@mail.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.updateUser(userId, updateDto);

        assertEquals("new@mail.com", user.getEmail());
        assertEquals("New Name", user.getName());
    }

    @Test
    @DisplayName("Ошибка, если... пользователь для обновления не найден")
    void updateUser_UserNotFound() {
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> userService.updateUser(userId, new RequestUpdateUserDto("e", "n")));
    }

    @Test
    @DisplayName("Сбор информации для нескольких заказов")
    void getInfoForOrders_Success() {
        Long orderId = 100L;
        Long vehicleId = 50L;
        Set<UUID> workerIds = Set.of(workerId);

        OrderUserMappingRequest request = new OrderUserMappingRequest(orderId, clientId, workerIds, vehicleId);

        User client = User.builder().id(clientId).name("Client").build();
        User worker = User.builder().id(workerId).name("Worker").build();
        Vehicle vehicle = new Vehicle();

        UserDto userDto = new UserDto(clientId, "Name", "Email");

        when(userRepository.findAllByIdIn(any())).thenReturn(List.of(client, worker));        when(carService.getVehiclesMap(anySet())).thenReturn(Map.of(vehicleId, vehicle));
        when(userMapper.toDto(any(User.class))).thenReturn(userDto);

        Map<Long, OrderInfoFromUserServiceDto> result = userService.getInfoForOrders(List.of(request));

        assertNotNull(result);
        assertTrue(result.containsKey(orderId));
        assertEquals(userDto, result.get(orderId).getClient());
        verify(carService, times(1)).getVehiclesMap(anySet());
    }

    @Test
    @DisplayName("Валидация списка работников - ошибка, если найдены не все")
    void validateWorkers_Fail() {
        Set<UUID> ids = Set.of(UUID.randomUUID(), UUID.randomUUID()); // Сет из UUID
        when(userRepository.findAllByIdIn(ids)).thenReturn(List.of(new User()));

        ValidationResponse response = userService.validateWorkers(ids);

        assertFalse(response.exists());
        assertNull(response.emails());
    }

    @Test
    @DisplayName("Добавление пользователя: Супер-админ может указать любую станцию")
    void addUser_SuperAdmin_SetsStationFromDto() {
        RequestAddUserDto dto = new RequestAddUserDto("Admin", "admin@st.com", "pass", "ADMIN", 99L);
        UserPrincipal principal = mock(UserPrincipal.class);

        var authority = new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_SUPERADMIN");
        doReturn(List.of(authority)).when(principal).authorities();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName(anyString())).thenReturn(Optional.of(new Role()));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");

        userService.addUser(dto, principal);

        verify(userRepository).save(argThat(user -> user.getWorkplaceId().equals(99L)));
    }

    @Test
    @DisplayName("Добавление пользователя: Обычный админ привязан к своей станции")
    void addUser_RegularAdmin_SetsStationFromPrincipal() {
        RequestAddUserDto dto = new RequestAddUserDto("Worker", "w@st.com", "pass", "WORKER", 99L);
        UserPrincipal principal = mock(UserPrincipal.class);

        when(principal.authorities()).thenReturn(Collections.emptyList());
        when(principal.stationId()).thenReturn(10L);

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName(anyString())).thenReturn(Optional.of(new Role()));

        userService.addUser(dto, principal);

        verify(userRepository).save(argThat(user -> user.getWorkplaceId().equals(10L)));
    }
}