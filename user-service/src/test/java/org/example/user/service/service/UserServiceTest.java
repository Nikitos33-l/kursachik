package org.example.user.service.service;

import feign.FeignException;
import org.example.order.service.api.common.client.OrderServiceClient;
import org.example.user.service.dto.response.UserShortResponse;
import org.example.user.service.entity.User;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    @DisplayName("Успешное получение всех работников станции")
    void getAllWorkers_Success() {
        Long stationId = 1L;
        User worker = new User();
        worker.setName("Ivan");

        UserShortResponse response = new UserShortResponse(1L, "Ivan","ivan@gmail.com");

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
        Long userId = 1L;

        userService.deleteUser(userId);

        verify(orderServiceClient, times(1)).deleteByUser(userId);
        verify(userRepository,times(1)).deleteById(userId);
    }

    @Test
    @DisplayName("Ошибка при удалении пользователя, если Feign клиент упал")
    void deleteUser_FeignException() {
        Long userId = 1L;
        doThrow(FeignException.class).when(orderServiceClient).deleteByUser(userId);

        assertThrows(RuntimeException.class, () -> userService.deleteUser(userId));
    }
}
