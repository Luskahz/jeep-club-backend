package com.jeepclub.backend.memberships.api.http.dto;

import com.jeepclub.backend.memberships.core.port.PendingFirstAccessUser;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado da aprovação com senha temporária. A senha é retornada apenas nesta resposta.")
public record TemporaryPasswordApprovalResponseDTO(
        @Schema(description = "Identificador do User criado em Identity.", example = "456")
        Long userId,
        @Schema(description = "Senha temporária exibida uma única vez ao administrador.", example = "Temp#Pass2026", accessMode = Schema.AccessMode.READ_ONLY)
        String temporaryPassword
) {

    public static TemporaryPasswordApprovalResponseDTO from(PendingFirstAccessUser result) {
        return new TemporaryPasswordApprovalResponseDTO(
                result.userId(),
                result.temporaryPassword()
        );
    }
}
