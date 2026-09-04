package com.jeepclub.backend.identity.api.http.dto.admin.user;

import com.jeepclub.backend.identity.api.module.UserStatus;
import com.jeepclub.backend.identity.core.application.query.user.AdminUserFilter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.time.LocalDate;

@Schema(name = "AdminUserFilter", description = "Filtros cadastrais da consulta administrativa de usuários.")
public record AdminUserFilterDTO(
        @Schema(description = "Identificador exato do usuário.", example = "1", minimum = "1", nullable = true)
        @Positive Long id,

        @Schema(description = "Nome ou parte do nome do usuário.", example = "Lucas", maxLength = 150,
                nullable = true)
        @Size(max = 150) String name,

        @Schema(description = "Data de nascimento exata do usuário.", example = "2000-05-17", format = "date",
                nullable = true)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthDate,

        @Schema(description = "E-mail ou parte do e-mail do usuário.", example = "lucas@example.com",
                maxLength = 180, nullable = true)
        @Size(max = 180) String email,

        @Schema(description = "CPF exato. Aceita 11 dígitos ou o formato com pontuação.",
                example = "52998224725", minLength = 11, maxLength = 14,
                pattern = "^(\\d{11}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2})$", nullable = true)
        @Pattern(
                regexp = "^(\\d{11}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2})$",
                message = "CPF deve estar no formato 00000000000 ou 000.000.000-00."
        )
        @CPF(message = "CPF inválido.") String cpf,

        @Schema(description = "RG exato; a consulta utiliza a representação canônica somente com dígitos.",
                example = "123456789", maxLength = 20, nullable = true)
        @Size(max = 20) String rg,

        @Schema(description = "Telefone ou parte do telefone; a consulta utiliza somente os dígitos.",
                example = "5511999999999", maxLength = 20, nullable = true)
        @Size(max = 20) String phoneNumber,

        @Schema(description = "Status administrativo do usuário.", example = "ACTIVE",
                allowableValues = {"ACTIVE", "DISABLED"}, nullable = true)
        UserStatus status,

        @Schema(description = "Instante mínimo de criação do usuário.", example = "2026-01-01T00:00:00Z",
                format = "date-time", nullable = true)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,

        @Schema(description = "Instante máximo de criação do usuário.", example = "2026-12-31T23:59:59Z",
                format = "date-time", nullable = true)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,

        @Schema(description = "Instante mínimo da última atualização.", example = "2026-01-01T00:00:00Z",
                format = "date-time", nullable = true)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant updatedFrom,

        @Schema(description = "Instante máximo da última atualização.", example = "2026-12-31T23:59:59Z",
                format = "date-time", nullable = true)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant updatedTo,

        @Schema(description = "Busca textual livre em nome, e-mail, CPF, RG e telefone.", example = "lucas",
                maxLength = 150, nullable = true)
        @Size(max = 150) String q
) {
    public AdminUserFilter toFilter() {
        return new AdminUserFilter(
                id, normalize(name), birthDate, normalize(email), digits(cpf), digits(rg),
                digits(phoneNumber), status, createdFrom, createdTo, updatedFrom, updatedTo,
                normalize(q)
        );
    }

    private static String normalize(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static String digits(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.replaceAll("\\D", "");
    }

    @Schema(hidden = true)
    @AssertTrue(message = "createdFrom must be before or equal to createdTo")
    public boolean isCreatedRangeValid() {
        return createdFrom == null || createdTo == null || !createdFrom.isAfter(createdTo);
    }

    @Schema(hidden = true)
    @AssertTrue(message = "updatedFrom must be before or equal to updatedTo")
    public boolean isUpdatedRangeValid() {
        return updatedFrom == null || updatedTo == null || !updatedFrom.isAfter(updatedTo);
    }
}
