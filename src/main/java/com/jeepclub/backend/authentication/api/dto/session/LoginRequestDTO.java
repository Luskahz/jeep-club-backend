package com.jeepclub.backend.authentication.api.dto.session;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

@Schema(description = "Dados necessários para autenticar um usuário.")
public record LoginRequestDTO(

        @Schema(
                description = "CPF do usuário.",
                example = "12345678909",
                minLength = 11,
                maxLength = 14,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "CPF é obrigatório.")
        @CPF(message = "CPF inválido.")
        String cpf,

        @Schema(
                description = "Senha do usuário.",
                example = "admin123",
                minLength = 8,
                maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Senha é obrigatória.")
        @Size(min = 8, max = 100, message = "Senha deve ter entre 8 e 100 caracteres.")
        String senha
) {
}