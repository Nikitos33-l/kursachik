package org.example.kursach;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.kursach.dto.Reguest_User_DTO;
import org.example.kursach.entity.Role;
import org.example.kursach.entity.User;
import org.example.kursach.repository.RoleRepository;
import org.example.kursach.repository.UserRepository;
import org.example.kursach.service.UserService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
        Reguest_User_DTO userDto = new Reguest_User_DTO("Иван","ivan@gmail.com","111","CLIENT");

        mockMvc.perform(
                post("/api/user/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto))
        ).andExpect(status().isOk());

        User user = userRepository.findByEmail("ivan@gmail.com");
        assertEquals("Иван",user.getName());
    }
}
