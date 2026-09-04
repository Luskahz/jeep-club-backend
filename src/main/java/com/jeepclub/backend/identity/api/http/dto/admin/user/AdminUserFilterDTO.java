package com.jeepclub.backend.identity.api.http.dto.admin.user;

import com.jeepclub.backend.identity.api.module.UserStatus;
import com.jeepclub.backend.identity.core.application.query.user.AdminUserFilter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.time.LocalDate;

@Schema(name = "AdminUserFilter", description = "Filtros cadastrais da consulta administrativa de usuários.")
public record AdminUserFilterDTO(
        @Positive Long id,
        @Size(max = 150) String name,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthDate,
        @Size(max = 180) String email,
        @CPF(message = "cpf must be valid") String cpf,
        @Size(max = 20) String rg,
        @Size(max = 20) String phoneNumber,
        UserStatus status,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant updatedFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant updatedTo,
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
