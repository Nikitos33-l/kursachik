package org.example.user.service.service;

import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.order.service.api.common.client.OrderServiceClient;
import org.example.user.service.dto.response.ResponseUserDto;
import org.example.user.service.dto.response.UserShortResponse;
import org.example.user.service.mapper.UserMapper;
import org.example.user.service.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final OrderServiceClient orderServiceClient;

    @Transactional
    public List<ResponseUserDto> getAll(Long stationId) {
        return userMapper.toListResponseUserDto(userRepository.findAll(stationId));
    }

    @Transactional
    public List<UserShortResponse> getAllWorkers(Long stationId) {
        return userMapper.toListShortResponse(userRepository.
                findAllByRole_NameAndWorkplaceId("WORKER",stationId));
    }

    @Transactional
    public void deleteUser(Long id) {
        try {
            orderServiceClient.deleteByUser(id);
        } catch (FeignException e) {
            throw new RuntimeException("Не удалось удалить связанные заказы. Попробуйте позже.");
        }
        orderServiceClient.deleteByUser(id);
    }
}
