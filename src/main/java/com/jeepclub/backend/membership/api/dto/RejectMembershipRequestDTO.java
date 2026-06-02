package com.jeepclub.backend.membership.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados opcionais para rejeição de uma solicitação de adesão.")
public record RejectMembershipRequestDTO(

        @Schema(
                description = "Motivo da rejeição. Quando informado, é incluído no e-mail de notificação ao candidato.",
                example = "Documentação incompleta ou inconsistente.",
                nullable = true
        )
        String reason

) {}