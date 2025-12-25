package org.example.kursach.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class Reguest_User_DTO {

    @NotNull
    private final String name;

    @NotNull
    @Email
    private final String email;

    @NotNull
    private final String password;

    @NotNull
    private final String role;

}
