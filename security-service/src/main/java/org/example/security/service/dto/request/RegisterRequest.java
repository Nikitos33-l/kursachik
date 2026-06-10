package org.example.security.service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Запрос на регистрацию нового сотрудника/администратора")
public record RegisterRequest(
        @Schema(description = "Имя и фамилия сотрудника", example = "Иван Иванов", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String name,

        @Schema(description = "Электронная почта (логин в системе)", example = "ivanov@stobackend.by", requiredMode = Schema.RequiredMode.REQUIRED)
        @Email @NotBlank
        String email,

        @Schema(description = "Пароль для создания учетной записи", example = "Secret_123P", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String password
) {}