package com.jeepclub.backend.authentication.api.dto.login;

import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.br.CPF;

public record LoginRequestDTO(
        @NotNull
        @CPF
        String cpf,
        @NotNull
        String senha) {
}
