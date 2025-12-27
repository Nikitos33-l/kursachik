package org.example.kursach.service;

import org.example.kursach.entity.Role;
import org.example.kursach.entity.User;
import org.example.kursach.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    JWTService JWT;

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    AuthService authService;

    @Test
    @DisplayName("Логин пользователя с правильным email и паролем")
    public void successful_login_user(){
        User user = create_user(1L,"111","Иван","ivan@gmail.com");
        Role role = new Role();
        role.setName("CLIENT");
        user.setRole(role);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(user);
        when(passwordEncoder.matches(user.getPassword(),user.getPassword())).thenReturn(true);

        authService.login(user);

        verify(userRepository,times(1)).findByEmail(user.getEmail());
        verify(passwordEncoder,times(1)).matches(user.getPassword(),user.getPassword());
    }

    @Test
    @DisplayName("Логин пользователя с неправильным email")
    public void Bad_email_login_user(){
        User user = create_user(1L,"111","Иван","ivan@gmail.com");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(null);

        assertThrows(BadCredentialsException.class,()->{
            authService.login(user);
        });
    }

    @Test
    @DisplayName("Логин пользователя с неправильным паролем")
    public void Bad_password_login_user(){
        User user = create_user(1L,"111","Иван","ivan@gmail.com");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(user);
        when(passwordEncoder.matches(user.getPassword(),user.getPassword())).thenReturn(false);

        assertThrows(BadCredentialsException.class,()->{
            authService.login(user);
        });
    }
    private User create_user(Long id,String password,String name,String email){
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setPassword(password);
        user.setEmail(email);
        return user;
    }
}
