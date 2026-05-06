package org.example.user.service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RequestUpdateUserDto(
        @NotBlank
        String name,

        @NotBlank
        @Email
        String email
) {
}
