package com.jeepclub.backend.authentication.api.dto.sessao;

import com.jeepclub.backend.authentication.core.application.results.MeResult;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Objects;

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

    public MeResponseDTO {
        authorities = List.copyOf(
                Objects.requireNonNull(authorities, "authorities cannot be null")
        );
    }

    public static MeResponseDTO from(
            MeResult result,
            List<String> authorities
    ) {
        Objects.requireNonNull(result, "result cannot be null");
        Objects.requireNonNull(authorities, "authorities cannot be null");

        return new MeResponseDTO(
                result.userId(),
                result.sessionId(),
                result.sessionActive(),
                result.expiresInSeconds(),
                authorities
        );
    }
}