package org.example.user.service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Данные для обновления основного профиля пользователя")
public record RequestUpdateUserDto(

        @NotBlank
        @Schema(description = "Новое имя (ФИО) пользователя", example = "Иванов Иван")
        String name,

        @NotBlank
        @Email
        @Schema(description = "Новый адрес электронной почты", example = "ivan_new@sto.by")
        String email
) {
}