package com.jeepclub.backend.authentication.api.http.dto.admin.user;

import com.jeepclub.backend.authentication.core.application.query.user.AdminUserFilter;
import com.jeepclub.backend.authentication.core.domain.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

@Schema(description = "Filtros disponíveis para consulta administrativa de usuários.")
public record AdminUserFilterDTO(

        @Schema(
                description = "Identificador exato do usuário.",
                example = "1"
        )
        @Positive
        Long id,

        @Schema(
                description = "Nome ou parte do nome do usuário.",
                example = "Lucas"
        )
        @Size(max = 150)
        String name,

        @Schema(
                description = "CPF exato do usuário, contendo apenas números.",
                example = "52998224725"
        )
        @Pattern(
                regexp = "\\d{11}",
                message = "cpf must contain exactly 11 digits"
        )
        String cpf,

        @Schema(
                description = "E-mail ou parte do e-mail do usuário.",
                example = "lucas@email.com"
        )
        @Size(max = 254)
        String email,

        @Schema(
                description = "Telefone ou parte do telefone do usuário.",
                example = "12999999999"
        )
        @Size(max = 20)
        String phone,

        @Schema(
                description = "Status administrativo do usuário.",
                example = "ACTIVE"
        )
        UserStatus status,

        @Schema(
                description = "Filtra usuários que precisam ou não alterar a senha.",
                example = "false"
        )
        Boolean passwordChangeRequired,

        @Schema(
                description = "Data mínima de criação do usuário.",
                example = "2026-01-01T00:00:00Z"
        )
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant createdFrom,

        @Schema(
                description = "Data máxima de criação do usuário.",
                example = "2026-12-31T23:59:59Z"
        )
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant createdTo,

        @Schema(
                description = "Data mínima da última atualização.",
                example = "2026-01-01T00:00:00Z"
        )
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant updatedFrom,

        @Schema(
                description = "Data máxima da última atualização.",
                example = "2026-12-31T23:59:59Z"
        )
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant updatedTo,

        @Schema(
                description = "Busca textual livre nos campos suportados pela consulta.",
                example = "lucas"
        )
        @Size(max = 150)
        String q
) {

    public AdminUserFilter toFilter() {
        return new AdminUserFilter(
                id,
                normalize(name),
                normalize(cpf),
                normalize(email),
                normalize(phone),
                status,
                passwordChangeRequired,
                createdFrom,
                createdTo,
                updatedFrom,
                updatedTo,
                normalize(q)
        );
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}