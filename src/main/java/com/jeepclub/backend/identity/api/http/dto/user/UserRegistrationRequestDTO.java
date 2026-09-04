package com.jeepclub.backend.identity.api.http.dto.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;

@Schema(name = "UserRegistrationRequest", description = "Dados necessários para registrar um novo usuário.")
@JsonIgnoreProperties(ignoreUnknown = false)
public record UserRegistrationRequestDTO(
        @NotBlank(message = "Nome é obrigatório.")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres.")
        @Schema(description = "Nome do usuário.", example = "Maria da Silva",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @Schema(description = "Data de nascimento do usuário.", example = "2000-05-17", nullable = true)
        LocalDate birthDate,

        @Email(message = "E-mail inválido.")
        @Size(max = 180, message = "E-mail deve ter no máximo 180 caracteres.")
        @Schema(description = "E-mail do usuário.", example = "maria@example.com", nullable = true)
        String email,

        @NotBlank(message = "CPF é obrigatório.")
        @Pattern(
                regexp = "^(\\d{11}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2})$",
                message = "CPF deve estar no formato 00000000000 ou 000.000.000-00."
        )
        @CPF(message = "CPF inválido.")
        @Schema(description = "CPF do usuário. Aceita somente os 11 dígitos ou o formato com pontuação.",
                example = "52998224725", minLength = 11, maxLength = 14,
                pattern = "^(\\d{11}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2})$",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String cpf,

        @Size(max = 20, message = "RG deve ter no máximo 20 caracteres.")
        @Schema(description = "RG, armazenado de forma canônica somente com dígitos.", example = "123456789",
                nullable = true)
        String rg,

        @NotBlank(message = "Senha é obrigatória.")
        @Size(min = 8, max = 100, message = "Senha deve ter entre 8 e 100 caracteres.")
        @Schema(description = "Senha inicial, com 8 a 100 caracteres.", example = "SenhaForte@123",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String password,

        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres.")
        @Schema(description = "Telefone, armazenado de forma canônica somente com dígitos.",
                example = "5511999999999", nullable = true)
        String phoneNumber
) {
}
