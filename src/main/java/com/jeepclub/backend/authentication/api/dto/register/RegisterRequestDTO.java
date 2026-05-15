package com.jeepclub.backend.authentication.api.dto.register;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.br.CPF;

/**
 * Data Transfer Object (DTO) focado EXCLUSIVAMENTE na recepção (Payload) da chamada da API (Inbound/Entrada).
 * O formato de Java Record é moderno e imutável para transporte de dados pela rede.
 */
public record RegisterRequestDTO(
    @NotBlank String name,
    LocalDate birthData,
    @Email String email,
    @NotBlank @CPF String cpf,
    String rg,
    @NotBlank String password,
    String phoneNumber
) {}
