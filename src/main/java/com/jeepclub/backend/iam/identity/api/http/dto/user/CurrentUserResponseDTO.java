package com.jeepclub.backend.iam.identity.api.http.dto.user;

import com.jeepclub.backend.iam.identity.api.module.UserDetails;
import com.jeepclub.backend.iam.identity.api.module.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Schema(name = "CurrentIdentityUserResponse", description = "Dados cadastrais do usuário autenticado.")
public record CurrentUserResponseDTO(
        @Schema(description = "ID estável do usuário.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,
        @Schema(description = "Nome do usuário.", example = "Maria da Silva", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @Schema(description = "Data de nascimento.", example = "2000-05-17", format = "date", nullable = true)
        LocalDate birthDate,
        @Schema(description = "E-mail cadastral.", example = "maria@example.com", format = "email", nullable = true)
        String email,
        @Schema(description = "CPF canônico com 11 dígitos.", example = "52998224725",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String cpf,
        @Schema(description = "RG canônico.", example = "123456789", nullable = true)
        String rg,
        @Schema(description = "Telefone canônico.", example = "5511999999999", nullable = true)
        String phoneNumber,
        @Schema(description = "Chave da foto no storage global; resolva via GET /media/images?key=...", nullable = true)
        String profilePhotoStorageKey,
        @Schema(description = "Estado administrativo do usuário.", requiredMode = Schema.RequiredMode.REQUIRED)
        UserStatus status,
        @Schema(description = "Instante de criação.", format = "date-time", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant createdAt,
        @Schema(description = "Instante de desativação administrativa.", format = "date-time", nullable = true)
        Instant disabledAt,
        @Schema(description = "Instante da última alteração.", format = "date-time", nullable = true)
        Instant updatedAt
) {
    public static CurrentUserResponseDTO from(UserDetails user) {
        Objects.requireNonNull(user, "user cannot be null");
        return new CurrentUserResponseDTO(
                user.id(), user.name(), user.birthDate(), user.email(), user.cpf(), user.rg(),
                user.phoneNumber(), user.profilePhotoStorageKey(),
                user.administrativelyActive() ? UserStatus.ACTIVE : UserStatus.DISABLED,
                user.createdAt(), user.disabledAt(), user.updatedAt()
        );
    }
}
