package com.jeepclub.backend.authentication.api.dto.me;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Dados da sessão autenticada do usuário.")
public record MeResponseDTO(

        @Schema(
                description = "Identificador único do usuário autenticado.",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Long userId,

        @Schema(
                description = "Identificador único da sessão autenticada.",
                example = "15",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Long sessionId,

        @Schema(
                description = "Indica se a sessão atual ainda está ativa.",
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        boolean sessionActive,

        @Schema(
                description = "Tempo restante de validade do access token, em segundos.",
                example = "900",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        long expiresInSeconds,

        @ArraySchema(
                schema = @Schema(
                        description = "Permissão atribuída ao usuário autenticado.",
                        example = "AUTHORIZATION_ROLE_READ"
                ),
                arraySchema = @Schema(
                        description = "Lista de authorities/permissões atuais do usuário autenticado.",
                        requiredMode = Schema.RequiredMode.REQUIRED
                )
        )
        List<String> authorities
) {
}