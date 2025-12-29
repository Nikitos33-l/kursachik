package org.example.kursach;

import org.example.kursach.entity.Role;
import org.example.kursach.entity.User;
import org.example.kursach.repository.RoleRepository;
import org.example.kursach.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuthIntegrationsTest extends KursachApplicationTests{

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;


    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    public void add_role(){
        roleRepository.deleteAll();
        Role client = new Role();
        client.setId(1L);
        client.setName("CLIENT");
        roleRepository.save(client);
    }

    @AfterEach
    public void delete_all(){
        userRepository.deleteAll();
        roleRepository.deleteAll();
    }

    @Test
    @DisplayName("Удачная регистрация пользователя")
    public void successful_registrate_user_test() throws Exception {
        User user_for_registrate = create_user("Иван","ivan@gmail.com","we123");

        mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user_for_registrate))
        ).andExpect(status().isOk());

        User user_from_bd = userRepository.findByEmail("ivan@gmail.com");

        assertTrue(passwordEncoder.matches("we123",user_from_bd.getPassword()));
        assertEquals(user_for_registrate.getName(),user_from_bd.getName());
    }

    private User create_user(String name,String email,String password){
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);
        return user;
    }
}
