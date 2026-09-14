package com.jeepclub.backend.health.api.http.dto;

import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.domain.model.MedicalProfile;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Confirmação administrativa sem reexposição de dados clínicos.")
public record MedicalProfileMutationResponse(
        @Schema(description = "Identificador do perfil médico.", example = "1")
        Long id,

        @Schema(description = "Tipo do owner do perfil médico.", example = "USER")
        MedicalProfileOwnerType ownerType,

        @Schema(description = "Identificador do owner do perfil médico.", example = "10")
        Long ownerId,

        @Schema(description = "Data e hora da criação ou última atualização do perfil.", format = "date-time")
        Instant updatedAt
) {
    public static MedicalProfileMutationResponse fromDomain(MedicalProfile profile) {
        return new MedicalProfileMutationResponse(
                profile.getId(),
                profile.getOwnerType(),
                profile.getOwnerId(),
                profile.getUpdatedAt()
        );
    }
}
