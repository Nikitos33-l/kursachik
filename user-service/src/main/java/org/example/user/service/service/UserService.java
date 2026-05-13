package org.example.user.service.service;

import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
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

    @Transactional(readOnly = true)
    public List<ResponseUserDto> getAll(Long stationId) {
        return userMapper.toListResponseUserDto(userRepository.findAll(stationId));
    }

    @Transactional(readOnly = true)
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
        userRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public UserShortResponse getInfo(Long id) {
        return userMapper.toShortResponse(getById(id));
    }

    @Transactional
    public void updateUser(Long id, RequestUpdateUserDto userDto) {
        User user = getById(id);
        user.setEmail(userDto.email().trim());
        user.setName(userDto.name().trim());
    }

    @Transactional(readOnly = true)
    public Map<Long, OrderInfoFromUserServiceDto> getInfoForOrders(List<OrderUserMappingRequest> request) {
        Set<Long> workerIds = request.stream()
                .flatMap(r-> r.workersId().stream()).collect(Collectors.toSet());

        Set<Long> clientIds = request.stream()
                .map(OrderUserMappingRequest::userId).collect(Collectors.toSet());

        Set<Long> vehiclesIds = request.stream()
                .map(OrderUserMappingRequest::vehicleId).collect(Collectors.toSet());

        Map<Long,User> workers = getUsersMap(workerIds);
        Map<Long,User> clients = getUsersMap(clientIds);
        Map<Long,Vehicle> vehicles = carService.getVehiclesMap(vehiclesIds);

        return request.stream().collect(Collectors.toMap(
                OrderUserMappingRequest::orderId,r->
                        new OrderInfoFromUserServiceDto(
                            getClientOfOrder(r.userId(),clients),
                            getWorkersOfOrder(r.workersId(),workers),
                            carService.getVehicleOfOrder(r.vehicleId(),vehicles)
                        )

        ));
    }

    private Map<Long,User> getUsersMap(Set<Long> ids){
        return userRepository.findAllByIdIn(ids).stream().collect(Collectors.toMap(
                User::getId,w->w
        ));
    }

    private UserDto getClientOfOrder(Long clientId,Map<Long,User> clients){
        return userMapper.toDto(clients.get(clientId));
    }

    private List<UserDto> getWorkersOfOrder(Set<Long> workerIds,Map<Long,User> workers){
        return workerIds.stream().map(id-> userMapper.toDto(workers.get(id))).toList();
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

    private Vehicle getVehicleById(Long id){
        return vehicleRepository.findById(id)
                .orElseThrow(()->new EntityNotFoundException("Автомобиля с таким id не существует"));
    }

    private User getById(Long id){
         return userRepository.
                findById(id).orElseThrow(()->new EntityNotFoundException("Нету пользователя с таким id"));
    }

    @Transactional(readOnly = true)
    public ValidationResponse validateWorkers(Set<Long> ids) {
        List<User> workers = userRepository.findAllByIdIn(ids);
        if(workers.size()!=ids.size()){
            return new ValidationResponse(false,null);
        }

        Map<Long,String> emails = workers.stream().collect(Collectors.toMap(
                User::getId,User::getEmail
        ));

        return new ValidationResponse(true,emails);

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

    @Transactional
    public void deleteByWorkplace(Long id) {
        userRepository.deleteById(id);
    }
}
