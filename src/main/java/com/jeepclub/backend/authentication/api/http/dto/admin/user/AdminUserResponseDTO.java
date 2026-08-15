package com.jeepclub.backend.authentication.api.http.dto.admin.user;

import com.jeepclub.backend.authentication.core.application.result.admin.user.AdminUserResult;
import com.jeepclub.backend.authentication.core.domain.enums.AccountStatus;
import com.jeepclub.backend.authentication.core.domain.enums.AuthenticationStatus;
import com.jeepclub.backend.authentication.core.domain.enums.CredentialStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Schema(
        name = "AdminUserResponse",
        description = "Resposta administrativa com dados seguros de um usuário."
)
public record AdminUserResponseDTO(

        @Schema(
                description = "Identificador único do usuário.",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Long id,

        @Schema(
                description = "Nome completo do usuário.",
                example = "Lucas Alves",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String name,

        @Schema(
                description = "CPF do usuário.",
                example = "12345678909",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String cpf,

        @Schema(
                description = "E-mail do usuário.",
                example = "lucas.alves@email.com",
                nullable = true
        )
        String email,

        @Schema(
                description = "Telefone do usuário.",
                example = "12991234567",
                nullable = true
        )
        String phone,

        @Schema(
                description = "Status da conta do usuário.",
                example = "ACTIVE",
                allowableValues = {"ACTIVE", "DISABLED"}
        )
        AccountStatus accountStatus,

        @Schema(
                description = "Status de autenticação do usuário.",
                example = "ENABLED",
                allowableValues = {"ENABLED", "LOCKED"}
        )
        AuthenticationStatus authenticationStatus,

        @Schema(
                description = "Status das credenciais do usuário.",
                example = "PERMANENT",
                allowableValues = {
                        "PERMANENT",
                        "PENDING_FIRST_ACCESS",
                        "CHANGE_REQUIRED"
                }
        )
        CredentialStatus credentialStatus,

        @Schema(
                description = "Indica se o usuário precisa trocar a senha no próximo acesso.",
                example = "false"
        )
        Boolean passwordChangeRequired,

        @Schema(
                description = "Data de criação do usuário.",
                example = "2026-06-04T17:44:38Z"
        )
        Instant createdAt,

        @Schema(
                description = "Data da última atualização do usuário.",
                example = "2026-06-04T18:10:00Z",
                nullable = true
        )
        Instant updatedAt
) {

    public static AdminUserResponseDTO from(AdminUserResult result) {
        Objects.requireNonNull(result, "result cannot be null");

        return new AdminUserResponseDTO(
                result.id(),
                result.name(),
                result.cpf(),
                result.email(),
                result.phone(),
                result.accountStatus(),
                result.authenticationStatus(),
                result.credentialStatus(),
                result.passwordChangeRequired(),
                result.createdAt(),
                result.updatedAt()
        );
    }

    public static List<AdminUserResponseDTO> from(
            List<AdminUserResult> results
    ) {
        Objects.requireNonNull(
                results,
                "results cannot be null"
        );

        return results.stream()
                .map(AdminUserResponseDTO::from)
                .toList();
    }
}