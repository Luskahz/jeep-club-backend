package com.jeepclub.backend.memberships.api.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompleteMemberActivationRequestDTO(
        @NotBlank
        @Schema(description = "Token bruto recebido no convite.", requiredMode = Schema.RequiredMode.REQUIRED)
        String token,
        @NotBlank
        @Size(min = 8, max = 72)
        @Schema(description = "Nova senha definitiva.", minLength = 8, maxLength = 72,
                requiredMode = Schema.RequiredMode.REQUIRED)
        String newPassword
) {
}
