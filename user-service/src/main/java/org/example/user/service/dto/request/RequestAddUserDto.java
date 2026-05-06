package org.example.user.service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RequestAddUserDto(
    @NotBlank
    String name,

    @NotBlank
    @Email
    String email,

    @NotBlank
    String password,

    @NotBlank
    String role,

    Long stationId
) {
}
