package com.jeepclub.backend.authentication.api.dtos.login;

import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.br.CPF;

public record UserLoginRequest(
        @NotNull
        @CPF
        String cpf,
        @NotNull
        String senha) {
}
