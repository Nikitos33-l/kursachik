package org.example.security.service.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.security.service.entity.Role;
import org.example.security.service.entity.User;
import org.example.security.service.repository.RoleRepository;
import org.example.security.service.repository.UserRepository;
import org.example.user.contracts.UserCreatedEvent;
import org.example.user.contracts.UserUpdateEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SecurityUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BlackListService blackListService;

    @Transactional
    public void deleteUser(UUID id){
        userRepository.deleteById(id);
        blackListService.blacklistUser(id.toString());
    }

    @Transactional
    public void updateUser(UserUpdateEvent event){
       User user = userRepository.findById(event.id()).orElseThrow(()-> new EntityNotFoundException("Пользователь с таким id не был найден"));
       user.setEmail(event.email());
       blackListService.blacklistUser(event.id().toString());
    }

    @Transactional
    public void createUser(UserCreatedEvent event){
        Role role = roleRepository.findByName(event.role()).orElseThrow(()->new EntityNotFoundException("Роль с таким названием не была найдена"));
        User user = User.builder().
                email(event.email()).id(event.id()).role(role).password(event.password()).workplaceId(event.workplaceId()).build();
        userRepository.save(user);
    }
}
