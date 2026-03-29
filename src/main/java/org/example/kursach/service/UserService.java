package org.example.kursach.service;


import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.example.kursach.dto.*;
import org.example.kursach.entity.Role;
import org.example.kursach.entity.Stations;
import org.example.kursach.entity.User;
import org.example.kursach.mapping.AllUserInfoDTOMap;
import org.example.kursach.mapping.OrderDTOMap;
import org.example.kursach.mapping.UserDTOMap;
import org.example.kursach.repository.RoleRepository;
import org.example.kursach.repository.StationsRepository;
import org.example.kursach.repository.UserRepository;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

@CacheConfig(cacheNames = "users")
@Service
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTService JWT;
    private final UserDTOMap userDTO_map;
    private final AllUserInfoDTOMap allUserInfoDTOMap;
    private final OrderDTOMap orderDTOMap;
    private final StationsRepository stationsRepository;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, JWTService JWT, PasswordEncoder passwordEncoder, UserDTOMap userDTOMap, AllUserInfoDTOMap allUserInfoDTOMap, OrderDTOMap orderDTOMap, StationsRepository stationsRepository){
        this.userRepository=userRepository;
        this.roleRepository=roleRepository;
        this.JWT=JWT;
        this.passwordEncoder = passwordEncoder;
        this.userDTO_map = userDTOMap;
        this.allUserInfoDTOMap = allUserInfoDTOMap;
        this.orderDTOMap = orderDTOMap;
        this.stationsRepository = stationsRepository;
    }

    @Cacheable
    public List<AllUserInfoDTO> findAll(){
       return userRepository.findAll().stream().map(allUserInfoDTOMap).toList();
    }

    @CacheEvict(allEntries = true)
    @Transactional
    public void deleteElement(Long id){
        User user = userRepository.findById(id).orElseThrow(()->new EntityNotFoundException("Объект не был найден"));
        user.getWorker_orders().forEach(o->o.getWorkers().remove(user));
        userRepository.deleteById(id);
    }

    @CacheEvict(allEntries = true)
    @Transactional
    public void update(Long id, UserDTO user) {
        User update_user = userRepository.findById(id).orElseThrow(()->new EntityNotFoundException("Объект не был найден"));
        User user_by_email = userRepository.findByEmail(user.getEmail());
        if(!update_user.equals(user_by_email) && user_by_email != null){
            throw new IllegalStateException();
        }
        update_user.setName(user.getName());
        update_user.setEmail(user.getEmail());
    }

    @Cacheable(key = "'workers'")
    public List<UserDTO> getAllWorkers() {
       return userRepository.findAllByRole_Name("WORKER").stream().map(userDTO_map).toList();
    }

    public UserDTO getInfo(Long id) {
        return userDTO_map.apply(userRepository.findById(id).orElseThrow(()->new EntityNotFoundException("Объект не был найден")));
    }

    @CacheEvict(allEntries = true)
    @Transactional
    public void addUser(ReguestUserDTO userDto, UserPrincipal currentUser) {
        if(userRepository.existsByEmail(userDto.getEmail())){
            throw new IllegalStateException();
        }
        User add_user = new User();
        add_user.setName(userDto.getName());
        add_user.setEmail(userDto.getEmail());
        Role role = roleRepository.findByName(userDto.getRole());
        if(role == null){
            throw new  EntityNotFoundException();
        }
        add_user.setRole(role);
        add_user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        add_user.setWorker_orders(new HashSet<>());
        if(isSuperAdmin(currentUser)){
            Stations station = stationsRepository.findById(userDto.getStationId()).orElseThrow(() -> new EntityNotFoundException());
            add_user.setWorkplace(station);
        }
        else{
            Stations station = stationsRepository.findById(currentUser.stationId()).orElseThrow(() -> new EntityNotFoundException());
            add_user.setWorkplace(station);
        }
        userRepository.save(add_user);
    }

    private boolean isSuperAdmin(UserPrincipal user){
        return user.authorities().stream().anyMatch(a -> a.getAuthority().equals("SUPERADMIN"));
    }

    public List<OrderDTO> findWorkerOrders(HttpServletRequest request) {
        String token = JWT.getToken(request);
        String email = JWT.getEmail(token);
        User user = userRepository.findByEmail(email);
        if(user == null){
            throw new EntityNotFoundException();
        }
        return user.getWorker_orders().stream().map(orderDTOMap).toList();
    }
}
