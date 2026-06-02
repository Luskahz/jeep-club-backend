package com.jeepclub.backend.health.api.dto;

import com.jeepclub.backend.health.core.domain.BloodType;
import com.jeepclub.backend.health.core.domain.MedicalProfile;
import com.jeepclub.backend.health.core.domain.MedicalProfileOwnerType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Resposta resumida de perfil médico para listagens administrativas.")
public record MedicalProfileSummaryResponse(
        @Schema(description = "ID do perfil médico.", example = "1")
        Long id,

        @Schema(description = "Tipo do proprietário do perfil médico.", example = "USER")
        MedicalProfileOwnerType ownerType,

        @Schema(description = "ID do usuário ou dependente proprietário do perfil médico.", example = "10")
        Long ownerId,

        @Schema(description = "Tipo sanguíneo.", example = "O_POSITIVE")
        BloodType bloodType,

        @Schema(description = "Data e hora da última atualização.")
        Instant updatedAt
) {
    public static MedicalProfileSummaryResponse fromDomain(MedicalProfile profile) {
        return new MedicalProfileSummaryResponse(
                profile.getId(),
                profile.getOwnerType(),
                profile.getOwnerId(),
                profile.getBloodType(),
                profile.getUpdatedAt()
        );
    }
}
