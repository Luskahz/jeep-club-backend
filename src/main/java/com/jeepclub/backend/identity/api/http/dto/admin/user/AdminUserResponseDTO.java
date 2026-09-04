package com.jeepclub.backend.identity.api.http.dto.admin.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jeepclub.backend.identity.api.module.UserStatus;
import com.jeepclub.backend.identity.core.application.result.admin.user.AdminUserResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Schema(name = "AdminUserResponse", description = "Dados cadastrais administrativos de um usuário.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdminUserResponseDTO(
        Long id,
        String name,
        LocalDate birthDate,
        String email,
        String cpf,
        String rg,
        String phoneNumber,
        String profilePhotoUrl,
        UserStatus status,
        Instant createdAt,
        Instant disabledAt,
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
