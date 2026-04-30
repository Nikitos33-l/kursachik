package org.example.kursach.mapping;


import org.example.kursach.dto.UserDTO;
import org.example.kursach.entity.User;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class UserDTOMap implements Function<User, UserDTO> {

    @Override
    public UserDTO apply(User user) {
        return new UserDTO(user.getId(),user.getName(), user.getEmail());
    }
}
