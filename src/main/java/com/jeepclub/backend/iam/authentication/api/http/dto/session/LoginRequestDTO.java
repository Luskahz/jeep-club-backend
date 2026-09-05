package com.jeepclub.backend.iam.authentication.api.http.dto.session;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

@Schema(description = "Dados necessários para autenticar um usuário.")
public record LoginRequestDTO(

        @Schema(
                description = "CPF do usuário. Aceita somente os 11 dígitos ou o formato com pontuação.",
                example = "52998224725",
                minLength = 11,
                maxLength = 14,
                pattern = "^(\\d{11}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2})$",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "CPF é obrigatório.")
        @Pattern(
                regexp = "^(\\d{11}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2})$",
                message = "CPF deve estar no formato 00000000000 ou 000.000.000-00."
        )
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
