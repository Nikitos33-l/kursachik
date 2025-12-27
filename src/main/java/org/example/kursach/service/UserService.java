package org.example.kursach.service;


import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.example.kursach.dto.All_User_infoDTO;
import org.example.kursach.dto.OrderDTO;
import org.example.kursach.dto.Reguest_User_DTO;
import org.example.kursach.dto.UserDTO;
import org.example.kursach.entity.Role;
import org.example.kursach.entity.User;
import org.example.kursach.mapping.All_user_infoDTO_map;
import org.example.kursach.mapping.OrderDTO_Map;
import org.example.kursach.mapping.UserDTO_Map;
import org.example.kursach.repository.RoleRepository;
import org.example.kursach.repository.UserRepository;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@CacheConfig(cacheNames = "users")
@Service
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTService JWT;
    private final UserDTO_Map userDTO_map;
    private final All_user_infoDTO_map allUserInfoDTOMap;
    private final OrderDTO_Map orderDTOMap;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, JWTService JWT, PasswordEncoder passwordEncoder, UserDTO_Map userDTOMap, All_user_infoDTO_map allUserInfoDTOMap, OrderDTO_Map orderDTOMap){
        this.userRepository=userRepository;
        this.roleRepository=roleRepository;
        this.JWT=JWT;
        this.passwordEncoder = passwordEncoder;
        this.userDTO_map = userDTOMap;
        this.allUserInfoDTOMap = allUserInfoDTOMap;
        this.orderDTOMap = orderDTOMap;
    }

    @Cacheable
    public List<All_User_infoDTO> findAll(){
       return userRepository.findAll().stream().map(allUserInfoDTOMap).toList();
    }

    @CacheEvict(allEntries = true)
    @Transactional
    public void delete_element(Long id){
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
    public List<UserDTO> get_all_workers() {
       return userRepository.findAllByRole_Name("WORKER").stream().map(userDTO_map).toList();
    }

    public UserDTO get_info(Long id) {
        return userDTO_map.apply(userRepository.findById(id).orElseThrow(()->new EntityNotFoundException("Объект не был найден")));
    }

    @CacheEvict(allEntries = true)
    @Transactional
    public void add_user(Reguest_User_DTO user) {
        if(userRepository.existsByEmail(user.getEmail())){
            throw new IllegalStateException();
        }
        User add_user = new User();
        add_user.setName(user.getName());
        add_user.setEmail(user.getEmail());
        Role role = roleRepository.findByName(user.getRole());
        if(role == null){
            throw new  EntityNotFoundException();
        }
        add_user.setRole(role);
        add_user.setPassword(passwordEncoder.encode(user.getPassword()));
        add_user.setWorker_orders(new HashSet<>());
        userRepository.save(add_user);
    }


    public List<OrderDTO> find_worker_orders(HttpServletRequest request) {
        String token = JWT.get_token(request);
        String email = JWT.get_email(token);
        User user = userRepository.findByEmail(email);
        if(user == null){
            throw new EntityNotFoundException();
        }
        return user.getWorker_orders().stream().map(orderDTOMap).toList();
    }
}
