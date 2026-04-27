package com.jeepclub.backend.authentication.api.dto.register;

import java.time.LocalDate;

/**
 * Data Transfer Object (DTO) focado EXCLUSIVAMENTE na recepção (Payload) da chamada da API (Inbound/Entrada).
 * O formato de Java Record é moderno e imutável para transporte de dados pela rede.
 */
public record RegisterRequestDTO(
    String name,
    LocalDate birthData,
    String email,
    String cpf,
    String rg,
    String password,
    String phoneNumber
) {}
