package org.example.user.service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Данные для создания нового пользователя системы")
public record RequestAddUserDto(

        @NotBlank
        @Schema(description = "Полное имя (ФИО) пользователя", example = "Иванов Иван Иванович")
        String name,

        @NotBlank
        @Email
        @Schema(description = "Электронная почта (уникальный логин)", example = "ivanov@sto.by")
        String email,

        @NotBlank
        @Schema(description = "Пароль для учетной записи", example = "SecureP@ss123")
        String password,

        @NotBlank
        @Schema(description = "Системная роль пользователя", example = "WORKER", allowableValues = {"ADMIN", "WORKER"})
        String role,

        @Schema(description = "Идентификатор СТО, к которой привязан сотрудник", example = "10", nullable = true)
        Long stationId
) {
}