package com.jeepclub.backend.authentication.api.http.dto.admin.user;

import com.fasterxml.jackson.annotation.JsonInclude;
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
        description = """
                Dados administrativos seguros de um usuário.
                Na listagem administrativa, propriedades podem ser omitidas
                quando não forem solicitadas por meio do parâmetro fields.
                """
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdminUserResponseDTO(

        @Schema(
                description = "Identificador único do usuário.",
                example = "1"
        )
        Long id,

        @Schema(
                description = "Nome completo do usuário.",
                example = "Lucas Alves"
        )
        String name,

        @Schema(
                description = "CPF do usuário.",
                example = "52998224725"
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
                description = "Status administrativo da conta do usuário.",
                example = "ACTIVE"
        )
        AccountStatus accountStatus,

        @Schema(
                description = "Status de autenticação do usuário.",
                example = "ENABLED"
        )
        AuthenticationStatus authenticationStatus,

        @Schema(
                description = "Status atual das credenciais do usuário.",
                example = "PERMANENT"
        )
        CredentialStatus credentialStatus,

        @Schema(
                description = "Indica se o usuário precisa alterar a senha antes da autenticação normal.",
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

    public static AdminUserResponseDTO from(
            AdminUserResult result
    ) {
        Objects.requireNonNull(
                result,
                "result cannot be null"
        );

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