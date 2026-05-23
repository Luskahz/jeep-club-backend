package com.jeepclub.backend.membershipKauan.api.dto.membership;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

@Schema(description = "Dados necessários para criar uma solicitação de associação ao Jeep Club.")
public record CreateMembershipApplicationRequestDTO(

        @Schema(
                description = "Nome completo do solicitante.",
                example = "Lucas Alves",
                maxLength = 150,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Nome é obrigatório.")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres.")
        String name,

        @Schema(
                description = "CPF do solicitante.",
                example = "12345678909",
                minLength = 11,
                maxLength = 14,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "CPF é obrigatório.")
        @CPF(message = "CPF inválido.")
        String cpf,

        @Schema(
                description = "E-mail de contato do solicitante.",
                example = "lucas.alves@email.com",
                maxLength = 180,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "E-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        @Size(max = 180, message = "E-mail deve ter no máximo 180 caracteres.")
        String email,

        @Schema(
                description = "Telefone de contato do solicitante.",
                example = "+5512999999999",
                maxLength = 20,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Telefone é obrigatório.")
        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres.")
        String phoneNumber,

        @Schema(
                description = "Mensagem enviada pelo solicitante.",
                example = "Tenho interesse em participar do Jeep Club e gostaria de receber mais informações.",
                maxLength = 2000,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Mensagem é obrigatória.")
        @Size(max = 2000, message = "Mensagem deve ter no máximo 2000 caracteres.")
        String message
) {
}