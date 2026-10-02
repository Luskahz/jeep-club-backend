package com.jeepclub.backend.iam.identity.api.http.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCurrentUserEmailRequestDTO(
        @NotBlank(message = "E-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        @Size(max = 180, message = "E-mail deve ter no máximo 180 caracteres.")
        @Schema(example = "usuario@example.com", maxLength = 180,
                requiredMode = Schema.RequiredMode.REQUIRED)
        String email
) {
    public UpdateCurrentUserEmailRequestDTO {
        if (email != null) {
            email = email.trim();
        }
    }
}
