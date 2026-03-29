package org.example.kursach.service;

import jakarta.transaction.Transactional;
import org.example.kursach.entity.User;
import org.example.kursach.repository.RoleRepository;
import org.example.kursach.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final JWTService JWT;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, RoleRepository roleRepository, JWTService jwt) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        JWT = jwt;
    }

    @Transactional
    public Map<String,String> login(User user){
        User user_bd= userRepository.
                findByEmail(user.getEmail());
        if (user_bd == null || !passwordEncoder.matches(user.getPassword(),user_bd.getPassword())){
            throw new BadCredentialsException("Неверные данные или пароль");
        }
        Long stationId = user.getWorkplace() != null ? user.getWorkplace().getId() : null;
        return getTokens(user.getEmail(),user_bd.getRole().getName(),stationId);
    }

    @Transactional
    @CacheEvict(cacheNames = "users",allEntries = true)
    public Map<String,String> save(User user){
        if(userRepository.findByEmail(user.getEmail()) != null){
            throw new IllegalStateException();
        }
        user.setRole(roleRepository.findByName("CLIENT"));
        String password=user.getPassword();
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
        return getTokens(user.getEmail(),"CLIENT",null);
    }

    private Map<String,String> getTokens(String email, String role, Long stationId){
        Map<String,String> map=new HashMap<>();
        map.put("Acesstoken",JWT.createAcesstoken(email,role,stationId));
        map.put("Refreshtoken",JWT.createRefreshtoken(email));
        return map;
    }

}
