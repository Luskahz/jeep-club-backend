package com.jeepclub.backend.identity.api.http.dto.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;

@Schema(description = "Dados necessários para registrar um novo usuário.")
@JsonIgnoreProperties(ignoreUnknown = false)
public record UserRegistrationRequestDTO(
        @NotBlank(message = "Nome é obrigatório.")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres.")
        String name,

        @Schema(description = "Data de nascimento do usuário.", example = "2000-05-17")
        LocalDate birthDate,

        @Email(message = "E-mail inválido.")
        @Size(max = 180, message = "E-mail deve ter no máximo 180 caracteres.")
        String email,

        @NotBlank(message = "CPF é obrigatório.")
        @CPF(message = "CPF inválido.")
        @Schema(description = "CPF formatado ou contendo apenas dígitos.", example = "529.982.247-25")
        String cpf,

        @Size(max = 20, message = "RG deve ter no máximo 20 caracteres.")
        String rg,

        @NotBlank(message = "Senha é obrigatória.")
        @Size(min = 8, max = 100, message = "Senha deve ter entre 8 e 100 caracteres.")
        String password,

        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres.")
        String phoneNumber
) {
}
