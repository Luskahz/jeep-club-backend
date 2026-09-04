package com.jeepclub.backend.identity.api.http.dto.admin.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jeepclub.backend.identity.api.module.UserStatus;
import com.jeepclub.backend.identity.core.application.result.admin.user.AdminUserResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Schema(
        name = "AdminUserResponse",
        description = """
                Dados cadastrais administrativos de um usuário.

                Na listagem administrativa, propriedades não solicitadas pelo parâmetro
                fields ou propriedades sem valor podem ser omitidas da resposta.
                """
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdminUserResponseDTO(
        @Schema(description = "Identificador único e estável do usuário.", example = "1")
        Long id,

        @Schema(description = "Nome do usuário.", example = "Lucas Alves")
        String name,

        @Schema(description = "Data de nascimento do usuário.", example = "2000-05-17", format = "date",
                nullable = true)
        LocalDate birthDate,

        @Schema(description = "E-mail cadastral do usuário.", example = "lucas@example.com", format = "email",
                nullable = true)
        String email,

        @Schema(description = "CPF canônico do usuário, contendo 11 dígitos.", example = "52998224725",
                minLength = 11, maxLength = 11, pattern = "^\\d{11}$")
        String cpf,

        @Schema(description = "RG canônico do usuário.", example = "123456789", nullable = true)
        String rg,

        @Schema(description = "Telefone canônico do usuário.", example = "5511999999999", nullable = true)
        String phoneNumber,

        @Schema(description = "URL da foto de perfil do usuário.", example = "https://cdn.example.com/user/1.jpg",
                nullable = true)
        String profilePhotoUrl,

        @Schema(description = "Status administrativo do usuário.", example = "ACTIVE",
                allowableValues = {"ACTIVE", "DISABLED"})
        UserStatus status,

        @Schema(description = "Instante de criação do usuário.", example = "2026-01-01T00:00:00Z",
                format = "date-time")
        Instant createdAt,

        @Schema(description = "Instante da desativação administrativa.", example = "2026-06-01T12:00:00Z",
                format = "date-time", nullable = true)
        Instant disabledAt,

        @Schema(description = "Instante da última atualização.", example = "2026-06-01T12:00:00Z",
                format = "date-time", nullable = true)
        Instant updatedAt
) {
    public static AdminUserResponseDTO from(AdminUserResult result) {
        Objects.requireNonNull(result, "result cannot be null");
        return new AdminUserResponseDTO(
                result.id(), result.name(), result.birthDate(), result.email(), result.cpf(),
                result.rg(), result.phoneNumber(), result.profilePhotoUrl(), result.status(),
                result.createdAt(), result.disabledAt(), result.updatedAt()
        );
    }
}
