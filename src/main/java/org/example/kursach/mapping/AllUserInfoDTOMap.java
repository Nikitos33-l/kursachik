package org.example.kursach.mapping;

import org.example.kursach.dto.AllUserInfoDTO;
import org.example.kursach.entity.User;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class AllUserInfoDTOMap implements Function<User, AllUserInfoDTO> {
    @Override
    public AllUserInfoDTO apply(User user) {
        return new AllUserInfoDTO(user.getId(), user.getName(), user.getEmail(), user.getRole().getName());
    }
}
