package org.example.kursach.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.example.kursach.dto.Reguest_User_DTO;
import org.example.kursach.dto.UserDTO;
import org.example.kursach.service.JWTService;
import org.example.kursach.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserService userService;

    @MockitoBean
    JWTService jwtService;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("Удачное обновление пользователя")
    @WithMockUser(username = "admin",roles = {"ADMIN"})
    public void successful_update_user_test() throws Exception {
        UserDTO userDTO = new UserDTO(null,"Иван","ivan@gmail.com");

        mockMvc.perform(
                put("/api/user/update/1")
                        .contentType(MediaType.APPLICATION_JSON).
                        content(objectMapper.writeValueAsString(userDTO))
        ).andExpect(status().isOk());

        verify(userService,times(1)).update(eq(1L),any(UserDTO.class));
    }

    @Test
    @DisplayName("Неудачное обновление пользователя плохой email")
    @WithMockUser(username = "admin",roles = {"ADMIN"})
    public void bad_email_update_user_test() throws Exception {
        UserDTO userDTO = new UserDTO(null,"Иван","jjjj");

        mockMvc.perform(
                put("/api/user/update/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO))
        ).andExpect(status().isBadRequest());

        verify(userService,times(0)).update(anyLong(),any(UserDTO.class));
    }

    @Test
    @DisplayName("Удачное удаление пользователя")
    @WithMockUser(username = "admin",roles = {"ADMIN"})
    public void successful_delete_test() throws Exception {
        mockMvc.perform(
                delete("/api/user/delete/1")
        ).andExpect(status().isOk());

        verify(userService,times(1)).delete_element(1L);
    }

    @Test
    @DisplayName("Неудачное удаление пользователя,несуществующий id")
    @WithMockUser(username = "admin",roles = {"ADMIN"})
    public void bad_id_delete_test() throws Exception {
       doThrow(new EntityNotFoundException()).when(userService).delete_element(99L);

       mockMvc.perform(
               delete("/api/user/delete/99")
       ).andExpect(status().isNotFound());

       verify(userService,times(1)).delete_element(99L);
    }

    @Test
    @DisplayName("Удачное добавление пользователя")
    @WithMockUser(username = "admin",roles = {"ADMIN"})
    public void successful_add_user_test() throws Exception {
        Reguest_User_DTO userDto = new Reguest_User_DTO("Иван","ivan@gmail.com","00000","ADMIN");

        mockMvc.perform(
                post("/api/user/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto))
        ).andExpect(status().isOk());

        verify(userService,times(1)).add_user(any(Reguest_User_DTO.class));
    }

    @Test
    @DisplayName("Неудачное обновление пользователя из-за некорректности данных")
    @WithMockUser(username = "admin",roles = {"ADMIN"})
    public void bad_argument_add_user_test() throws Exception {
        Reguest_User_DTO userDto = new Reguest_User_DTO("","ivanil.com","00000","ADMIN");

        mockMvc.perform(
                post("/api/user/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto))
        ).andExpect(status().isBadRequest());

        verify(userService,times(0)).add_user(any(Reguest_User_DTO.class));
    }

}
