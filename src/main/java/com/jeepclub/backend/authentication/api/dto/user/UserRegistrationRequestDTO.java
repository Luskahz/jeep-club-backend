package com.jeepclub.backend.authentication.api.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;

@Schema(description = "Dados necessários para registrar um novo usuário.")
public record UserRegistrationRequestDTO(

        @Schema(
                description = "Nome completo do usuário.",
                example = "Lucas Alves",
                maxLength = 150,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Nome é obrigatório.")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres.")
        String name,

        @Schema(
                description = "Data de nascimento do usuário.",
                example = "2000-05-17",
                nullable = true
        )
        @JsonProperty("birthData")
        LocalDate birthDate,

        @Schema(
                description = "E-mail do usuário.",
                example = "lucas.alves@email.com",
                maxLength = 180,
                nullable = true
        )
        @Email(message = "E-mail inválido.")
        @Size(max = 180, message = "E-mail deve ter no máximo 180 caracteres.")
        String email,

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
                description = "RG do usuário.",
                example = "12.345.678-9",
                maxLength = 20,
                nullable = true
        )
        @Size(max = 20, message = "RG deve ter no máximo 20 caracteres.")
        String rg,

        @Schema(
                description = "Senha do usuário.",
                example = "Senha@123",
                minLength = 8,
                maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Senha é obrigatória.")
        @Size(min = 8, max = 100, message = "Senha deve ter entre 8 e 100 caracteres.")
        String password,

        @Schema(
                description = "Telefone de contato do usuário.",
                example = "+5512999999999",
                maxLength = 20,
                nullable = true
        )
        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres.")
        String phoneNumber
) {
}
