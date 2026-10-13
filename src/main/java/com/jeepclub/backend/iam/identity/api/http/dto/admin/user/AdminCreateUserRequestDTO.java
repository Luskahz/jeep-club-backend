package com.jeepclub.backend.iam.identity.api.http.dto.admin.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.jeepclub.backend.iam.identity.api.module.UserActivationMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = false)
@Schema(description = "Cadastro administrativo de um novo usuário. A modalidade de ativação deve ser escolhida explicitamente.")
public record AdminCreateUserRequestDTO(
        @NotBlank @Size(max = 150) String name,
        @Schema(format = "date", nullable = true) LocalDate birthDate,
        @Email @Size(max = 180) @Schema(nullable = true) String email,
        @NotBlank @Pattern(regexp = "^(\\d{11}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2})$") @CPF String cpf,
        @Size(max = 20) @Schema(nullable = true) String rg,
        @Size(max = 20) @Schema(nullable = true) String phoneNumber,
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UserActivationMethod activationMethod
) {
}
