package org.example.kursach.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class ReguestUserDTO {

    @NotBlank
    private final String name;

    @NotBlank
    @Email
    private final String email;

    @NotBlank
    private final String password;

    @NotBlank
    private final String role;

    private final Long stationId;


}
