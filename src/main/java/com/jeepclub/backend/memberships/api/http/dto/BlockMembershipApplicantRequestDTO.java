package com.jeepclub.backend.memberships.api.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para rejeitar uma solicitação e bloquear o CPF do solicitante.")
public record BlockMembershipApplicantRequestDTO(

        @NotBlank(message = "Motivo do bloqueio é obrigatório.")
        @Size(max = 2000, message = "Motivo do bloqueio deve ter no máximo 2000 caracteres.")
        @Schema(description = "Motivo administrativo do bloqueio.", example = "Tentativas recorrentes com dados inconsistentes.")
        String reason
) {}
