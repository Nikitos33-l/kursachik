package org.example.kursach.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class All_User_infoDTO {
    private final Long id;
    private final String name;
    private final String email;
    private final String role;
}
