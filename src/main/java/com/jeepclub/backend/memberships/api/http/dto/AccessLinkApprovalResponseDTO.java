package com.jeepclub.backend.memberships.api.http.dto;

import com.jeepclub.backend.memberships.core.port.AccessLinkApproval;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado da aprovação com convite de ativação enviado por e-mail.")
public record AccessLinkApprovalResponseDTO(
        @Schema(description = "Identificador do User criado em Identity.", example = "456")
        Long userId,
        @Schema(description = "Credencial bearer exibida uma única vez. Não armazenar ou registrar.",
                example = "https://frontend.example/membership/activate?token=REDACTED")
        String activationLink
) {

    @Override public String toString() { return "AccessLinkApprovalResponseDTO[userId=" + userId + ", activationLink=[REDACTED]]"; }

    public static AccessLinkApprovalResponseDTO from(AccessLinkApproval result) {
        return new AccessLinkApprovalResponseDTO(result.userId(), result.activationLink());
    }
}
