package com.jeepclub.backend.authentication.api.dto.recovery;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.br.CPF;

@Schema(description = "Dados necessários para solicitar recuperação de senha por e-mail.")
public record PasswordRecoveryRequestDTO(

        @Schema(
                description = "CPF do usuário que solicitou recuperação de senha.",
                example = "12345678909",
                minLength = 11,
                maxLength = 14,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "CPF é obrigatório.")
        @CPF(message = "CPF inválido.")
        String cpf
) {
}