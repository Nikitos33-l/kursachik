package org.example.kursach.mapping;

import org.example.kursach.dto.All_User_infoDTO;
import org.example.kursach.entity.User;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class All_user_infoDTO_map implements Function<User, All_User_infoDTO> {
    @Override
    public All_User_infoDTO apply(User user) {
        return new All_User_infoDTO(user.getId(), user.getName(), user.getEmail(), user.getRole().getName());
    }
}
