package org.example.kursach;

import org.example.kursach.dto.ReguestUserDTO;
import org.example.kursach.entity.Role;
import org.example.kursach.entity.User;
import org.example.kursach.repository.RoleRepository;
import org.example.kursach.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserIntegretionTest extends KursachApplicationTests {

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;


    @BeforeEach
    public void add_role(){
        Role client = new Role();
        client.setId(1L);
        client.setName("CLIENT");
        roleRepository.save(client);
    }

    @AfterEach
    public void delete(){
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Успешный интеграционный тест добавления пользователя")
    @WithMockUser(username = "admin",roles = {"ADMIN"})
    public void successful_add_user_test() throws Exception {
        ReguestUserDTO userDto = new ReguestUserDTO("Иван","ivan@gmail.com","111","CLIENT",1L);

        mockMvc.perform(
                post("/api/user/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto))
        ).andExpect(status().isOk());

        User user = userRepository.findByEmail("ivan@gmail.com");
        assertEquals("Иван",user.getName());
    }
}
