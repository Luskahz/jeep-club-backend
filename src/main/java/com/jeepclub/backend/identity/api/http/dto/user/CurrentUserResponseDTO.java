package com.jeepclub.backend.identity.api.http.dto.user;

import com.jeepclub.backend.identity.api.module.UserDetails;
import com.jeepclub.backend.identity.api.module.UserStatus;
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
        @Schema(description = "Data de nascimento.", example = "2000-05-17", nullable = true)
        LocalDate birthDate,
        @Schema(description = "E-mail cadastral.", example = "maria@example.com", nullable = true)
        String email,
        @Schema(description = "CPF canônico com 11 dígitos.", example = "52998224725",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String cpf,
        @Schema(description = "RG canônico.", example = "123456789", nullable = true)
        String rg,
        @Schema(description = "Telefone canônico.", example = "5511999999999", nullable = true)
        String phoneNumber,
        @Schema(description = "URL da foto de perfil.", nullable = true)
        String profilePhotoUrl,
        @Schema(description = "Estado administrativo do usuário.", requiredMode = Schema.RequiredMode.REQUIRED)
        UserStatus status,
        @Schema(description = "Instante de criação.", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant createdAt,
        @Schema(description = "Instante de desativação administrativa.", nullable = true)
        Instant disabledAt,
        @Schema(description = "Instante da última alteração.", nullable = true)
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
