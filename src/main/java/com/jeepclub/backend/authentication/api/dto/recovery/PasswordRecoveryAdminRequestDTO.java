package com.jeepclub.backend.authentication.api.dto.recovery;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Dados necessários para gerar recuperação de senha por administrador.")
public record PasswordRecoveryAdminRequestDTO(

        @Schema(
                description = "Identificador do usuário que terá a senha recuperada.",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "ID do usuário é obrigatório.")
        @Positive(message = "ID do usuário deve ser positivo.")
        Long targetUserId,

        @Schema(
                description = "Indica se deve ser gerada uma senha provisória em texto claro.",
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Opção de gerar senha provisória é obrigatória.")
        Boolean generateTempPassword
) {
}