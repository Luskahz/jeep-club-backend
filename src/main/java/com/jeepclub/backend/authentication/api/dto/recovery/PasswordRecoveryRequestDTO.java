package com.jeepclub.backend.authentication.api.dto.recovery;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.br.CPF;

@Schema(
        name = "PasswordRecoveryRequest",
        description = "Dados necessários para criar, consultar ou avançar uma solicitação de recuperação de senha."
)
public record PasswordRecoveryRequestDTO(

        @Schema(
                description = "CPF do usuário que está solicitando recuperação de senha. Aceita somente números ou o formato com pontuação.",
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
        String cpf
) {
}