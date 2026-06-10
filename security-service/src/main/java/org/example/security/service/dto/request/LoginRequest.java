package org.example.security.service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Запрос на вход в систему (авторизация)")
public record LoginRequest(
        @Schema(description = "Электронная почта пользователя", example = "ivanov@stobackend.by", requiredMode = Schema.RequiredMode.REQUIRED)
        @Email @NotBlank
        String email,

        @Schema(description = "Пароль пользователя", example = "Secret_123P", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String password
) {}