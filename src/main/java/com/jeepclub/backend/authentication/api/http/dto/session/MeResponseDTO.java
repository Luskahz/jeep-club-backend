package com.jeepclub.backend.authentication.api.http.dto.session;

import com.jeepclub.backend.authentication.core.application.result.MeResult;
import com.jeepclub.backend.authentication.core.domain.enums.AccountStatus;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Schema(description = "Dados da sessão autenticada e do cadastro do usuário.")
public record MeResponseDTO(

        @Schema(
                description = "Identificador único do usuário autenticado.",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Long userId,

        @Schema(
                description = "Nome completo do usuário autenticado.",
                example = "Lucas Alves",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String userName,

        @Schema(
                description = "Data de nascimento do usuário.",
                example = "2006-02-10",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        LocalDate birthDate,

        @Schema(
                description = "E-mail cadastrado do usuário.",
                example = "lucas@email.com",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String email,

        @Schema(
                description = "CPF cadastrado do usuário.",
                example = "12345678900",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String cpf,

        @Schema(
                description = "RG cadastrado do usuário.",
                example = "123456789",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String rg,

        @Schema(
                description = "Número de telefone cadastrado do usuário.",
                example = "12999999999",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String phoneNumber,

        @Schema(
                description = "URL da foto de perfil do usuário.",
                example = "https://example.com/profile/1.jpg"
        )
        String profilePhotoUrl,

        @Schema(
                description = "Status atual da conta do usuário.",
                example = "ACTIVE",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        AccountStatus accountStatus,

        @Schema(
                description = "Data e hora de criação do cadastro.",
                example = "2026-08-01T13:00:00Z",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Instant createdAt,

        @Schema(
                description = "Data e hora da última atualização do cadastro.",
                example = "2026-08-14T20:00:00Z",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Instant updatedAt,

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
                result.userName(),
                result.birthDate(),
                result.email(),
                result.cpf(),
                result.rg(),
                result.phoneNumber(),
                result.profilePhotoUrl(),
                result.accountStatus(),
                result.createdAt(),
                result.updatedAt(),
                result.sessionId(),
                result.sessionActive(),
                result.expiresInSeconds(),
                authorities
        );
    }
}