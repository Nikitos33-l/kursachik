package org.example.user.service.service;

import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.order.service.api.common.client.OrderServiceClient;
import org.example.securitycommon.UserPrincipal;
import org.example.user.service.dto.request.RequestAddUserDto;
import org.example.user.service.dto.request.RequestUpdateUserDto;
import org.example.user.service.dto.response.ResponseUserDto;
import org.example.user.service.dto.response.UserShortResponse;
import org.example.user.service.entity.Role;
import org.example.user.service.entity.User;
import org.example.user.service.mapper.UserMapper;
import org.example.user.service.repository.RoleRepository;
import org.example.user.service.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final OrderServiceClient orderServiceClient;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

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

    @Transactional
    public UserShortResponse getInfo(Long id) {
        return userMapper.toShortResponse(getById(id));
    }

    @Transactional
    public void updateUser(Long id, RequestUpdateUserDto userDto) {
        User user = getById(id);
        user.setEmail(userDto.email().trim());
        user.setName(userDto.name().trim());
    }

    private User getById(Long id){
         return userRepository.
                findById(id).orElseThrow(()->new EntityNotFoundException("Нету пользователя с таким id"));
    }

    @Transactional
    public void addUser(RequestAddUserDto userDto, UserPrincipal userPrincipal) {
        checkUserExists(userDto.email());
        Role role = getRoleByName(userDto.role());
        Long stationId = getStationId(userDto,userPrincipal);

        User user = User.builder()
                .name(userDto.name())
                .email(userDto.email())
                .role(role).password(passwordEncoder.encode(userDto.password()))
                .workplaceId(stationId).build();

        userRepository.save(user);
    }

    private void checkUserExists(String email){
        if(userRepository.existsByEmail(email)){
            throw new IllegalStateException("Пользователь с таким email уже существует");
        }
    }

    private Role getRoleByName(String name){
        return roleRepository.findByName(name)
                .orElseThrow(()-> new EntityNotFoundException("Роли с таким именем не существует"));
    }

    private Long getStationId(RequestAddUserDto userDto,UserPrincipal userPrincipal){
        if(isSuperAdmin(userPrincipal)){
            return userDto.stationId();
        }
        else{
            return userPrincipal.stationId();
        }
    }

    private boolean isSuperAdmin(UserPrincipal user){
        return user.authorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN"));
    }


}
