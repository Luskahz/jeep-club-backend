package com.jeepclub.backend.memberships.api.http.dto;

import com.jeepclub.backend.memberships.core.port.PendingFirstAccessLink;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado da aprovação com link para definir a primeira senha.")
public record AccessLinkApprovalResponseDTO(
        @Schema(description = "Identificador do User criado em Identity.", example = "456")
        Long userId,
        @Schema(description = "Link temporário para definição da primeira senha.", example = "https://app.example.com/definir-senha?token=example-token", format = "uri", accessMode = Schema.AccessMode.READ_ONLY)
        String accessLink
) {

    public static AccessLinkApprovalResponseDTO from(PendingFirstAccessLink result) {
        return new AccessLinkApprovalResponseDTO(
                result.userId(),
                result.accessLink()
        );
    }
}
