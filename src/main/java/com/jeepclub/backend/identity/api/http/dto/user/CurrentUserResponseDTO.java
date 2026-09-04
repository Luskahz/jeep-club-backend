package com.jeepclub.backend.identity.api.http.dto.user;

import com.jeepclub.backend.identity.api.module.UserDetails;
import com.jeepclub.backend.identity.api.module.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Schema(name = "CurrentIdentityUserResponse", description = "Dados cadastrais do usuário autenticado.")
public record CurrentUserResponseDTO(
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
    public static CurrentUserResponseDTO from(UserDetails user) {
        Objects.requireNonNull(user, "user cannot be null");
        return new CurrentUserResponseDTO(
                user.id(), user.name(), user.birthDate(), user.email(), user.cpf(), user.rg(),
                user.phoneNumber(), user.profilePhotoUrl(),
                user.administrativelyActive() ? UserStatus.ACTIVE : UserStatus.DISABLED,
                user.createdAt(), user.disabledAt(), user.updatedAt()
        );
    }
}
