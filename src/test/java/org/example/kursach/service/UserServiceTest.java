package org.example.kursach.service;

import org.example.kursach.dto.All_User_infoDTO;
import org.example.kursach.entity.User;
import org.example.kursach.mapping.All_user_infoDTO_map;
import org.example.kursach.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private All_user_infoDTO_map allUserInfoDTOMap;

    @InjectMocks
    private UserService userService;


    @Test
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

    private User create_user(Long id,String password,String name,String email){
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setPassword(password);
        user.setEmail(email);
        return user;
    }
}
