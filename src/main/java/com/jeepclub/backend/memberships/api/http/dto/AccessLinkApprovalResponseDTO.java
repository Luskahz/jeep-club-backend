package com.jeepclub.backend.memberships.api.http.dto;

import com.jeepclub.backend.memberships.core.port.PendingFirstAccessIdentity;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado da aprovação com convite de ativação enviado por e-mail.")
public record AccessLinkApprovalResponseDTO(
        @Schema(description = "Identificador do User criado em Identity.", example = "456")
        Long userId
) {

    public static AccessLinkApprovalResponseDTO from(PendingFirstAccessIdentity result) {
        return new AccessLinkApprovalResponseDTO(result.userId());
    }
}
