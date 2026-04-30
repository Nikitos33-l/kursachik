package org.example.kursach;

import jakarta.transaction.Transactional;
import org.example.kursach.dto.ReguestUserDTO;
import org.example.kursach.dto.UserPrincipal;
import org.example.kursach.entity.Role;
import org.example.kursach.entity.Stations;
import org.example.kursach.entity.User;
import org.example.kursach.repository.RoleRepository;
import org.example.kursach.repository.StationsRepository;
import org.example.kursach.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
public class UserIntegretionTest extends KursachApplicationTests {

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    StationsRepository stationsRepository;



    @BeforeEach
    public void addRole(){
        roleRepository.deleteAll();
        Role client = new Role();
        client.setId(1L);
        client.setName("CLIENT");
        roleRepository.save(client);
    }

    @BeforeEach
    public void addStations(){
        stationsRepository.deleteAll();
        Stations stations = new Stations();
        stationsRepository.save(stations);
    }

    @AfterEach
    public void deleteAll(){
        userRepository.deleteAll();
        roleRepository.deleteAll();
        stationsRepository.deleteAll();
    }

    @Test
    @DisplayName("Успешный интеграционный тест добавления пользователя")
    public void successful_add_user_test() throws Exception {
        UserPrincipal principal = new UserPrincipal(
                "admin@gmail.com",
                1L,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        ReguestUserDTO userDto = new ReguestUserDTO("Иван", "ivan@gmail.com", "111", "CLIENT", 1L);

        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.authorities());

        mockMvc.perform(
                post("/api/user/add")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto))
        ).andExpect(status().isOk());

        User user = userRepository.findByEmail("ivan@gmail.com");
        assertNotNull(user);
        assertEquals("Иван", user.getName());
    }
}
