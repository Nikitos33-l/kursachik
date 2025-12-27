package org.example.kursach.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.kursach.dto.All_User_infoDTO;
import org.example.kursach.dto.Reguest_User_DTO;
import org.example.kursach.dto.UserDTO;
import org.example.kursach.entity.Role;
import org.example.kursach.entity.User;
import org.example.kursach.mapping.All_user_infoDTO_map;
import org.example.kursach.repository.RoleRepository;
import org.example.kursach.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private All_user_infoDTO_map allUserInfoDTOMap;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JWTService jwtService;

    @InjectMocks
    private UserService userService;


    @Test
    @DisplayName("Нахождение всех пользователей")
    public void findAlltest(){
        User first_user = create_user(1L,"1111","Иван","ivan@gmail.com");
        User second_user = create_user(2L,"243132","Никита","nikita@gmail.com");
        All_User_infoDTO first_user_dto = new All_User_infoDTO(1L,"Иван","ivan@gmail.com","CLIENT");
        All_User_infoDTO second_user_dto = new All_User_infoDTO(2L,"Никита","nikita@gmail.com","CLIENT");
        List<User> users = List.of(first_user,second_user);
        List<All_User_infoDTO> dto_users = List.of(first_user_dto,second_user_dto);
        when(userRepository.findAll()).thenReturn(users);
        when(allUserInfoDTOMap.apply(first_user)).thenReturn(first_user_dto);
        when(allUserInfoDTOMap.apply(second_user)).thenReturn(second_user_dto);
        List<All_User_infoDTO> result = userService.findAll();

        assertNotNull(result);
        assertEquals(dto_users,result);
        assertEquals(dto_users.getFirst(),result.getFirst());
        assertEquals(dto_users.get(1),result.get(1));

        verify(userRepository,times(1)).findAll();
        verify(allUserInfoDTOMap,times(2)).apply(any(User.class));
    }


    @Test
    @DisplayName("Удаление пользователя с существующим id")
    public void delete_elementtest(){
        Long id = 1L;
        User user = new User();
        user.setId(id);
        user.setWorker_orders(new HashSet<>());

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        userService.delete_element(id);

        verify(userRepository,times(1)).findById(id);
        verify(userRepository,times((1))).deleteById(id);
    }

    @Test
    @DisplayName("Удаление пользователя с несуществующим id")
    public void throw_exception_delete_elementtest(){
        Long id = 999L;

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,()->{
            userService.delete_element(id);
        });

        assertEquals("Объект не был найден",exception.getMessage());
        verify(userRepository,times(1)).findById(id);
        verify(userRepository,never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("Обновление пользователя с существующим id и незанятым email")
    public void successful_update_user_test(){
        Long id = 1L;
        UserDTO userDTO = new UserDTO(1L,"Иван","example@gmail.com");
        User user = create_user(1L,"","Иван","example@gmail.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail(userDTO.getEmail())).thenReturn(null);

        userService.update(id,userDTO);

        verify(userRepository,times(1)).findById(id);
        verify(userRepository,times(1)).findByEmail(userDTO.getEmail());
    }

    @Test
    @DisplayName("Обновление пользователя с несуществующим id")
    public void throw_NotFoundException_update_user_test(){
        Long id = 999L;
        UserDTO userDTO = new UserDTO(null,"Иван","example@gmail.com");
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,()->userService.update(id,userDTO));

        verify(userRepository,times(1)).findById(id);
        verify(userRepository,times(0)).findByEmail(anyString());
    }

    @Test
    @DisplayName("Обновление пользователя с существующим id но занятым email")
    public void throw_IllegalStateException_update_user_test(){
        Long id = 1L;
        UserDTO userDTO = new UserDTO(1L,"Иван","example@gmail.com");
        User user = create_user(1L,"","Иван","example@gmail.com");
        User user_with_new_email = create_user(2L,"","Роман","example@gmail.com");
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail(userDTO.getEmail())).thenReturn(user_with_new_email);

        assertThrows(IllegalStateException.class,()->userService.update(id,userDTO));
        verify(userRepository,times(1)).findById(id);
        verify(userRepository,times(1)).findByEmail(userDTO.getEmail());
    }

    @Test
    @DisplayName("Добавление пользователя с незанятым email и существующей ролью")
    public void successful_add_user_test(){
        Reguest_User_DTO userDto = new Reguest_User_DTO("Иван","ivan@gmail.com","1111","CLIENT");
        when(userRepository.existsByEmail(userDto.getEmail())).thenReturn(false);
        when(roleRepository.findByName(userDto.getRole())).thenReturn(new Role());

        userService.add_user(userDto);

        verify(userRepository,times(1)).existsByEmail(userDto.getEmail());
        verify(roleRepository,times(1)).findByName(userDto.getRole());
        verify(passwordEncoder,times(1)).encode(userDto.getPassword());
        verify(userRepository,times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Добавление пользователя с занятым email")
    public void throw_IllegalStateException_add_user_test(){
        Reguest_User_DTO userDto = new Reguest_User_DTO("Иван","ivan@gmail.com","1111","CLIENT");
        when(userRepository.existsByEmail(userDto.getEmail())).thenReturn(true);

        assertThrows(IllegalStateException.class,()->{
            userService.add_user(userDto);
        });

        verify(userRepository,times(1)).existsByEmail(userDto.getEmail());
        verify(roleRepository,times(0)).findByName(userDto.getRole());
        verify(userRepository,times(0)).save(any(User.class));
    }

    @Test
    @DisplayName("Добавление пользователя с несуществующей ролью")
    public void throw_EntityNotFoundException_add_user_test(){
        Reguest_User_DTO userDto = new Reguest_User_DTO("Иван","ivan@gmail.com","1111","CLIENT");
        when(userRepository.existsByEmail(userDto.getEmail())).thenReturn(false);
        when(roleRepository.findByName(userDto.getRole())).thenReturn(null);

        assertThrows(EntityNotFoundException.class,()->{
            userService.add_user(userDto);
        });

        verify(userRepository,times(1)).existsByEmail(userDto.getEmail());
        verify(roleRepository,times(1)).findByName(userDto.getRole());
        verify(userRepository,times(0)).save(any(User.class));
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
