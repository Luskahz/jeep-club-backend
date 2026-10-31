package com.jeepclub.backend.health.api.http.dto;

import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.application.result.AdminMedicalProfilePresentation;
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

        @Schema(description = "Nome atual do usuário ou dependente proprietário.")
        String ownerName,

        @Schema(description = "Nome atual do usuário responsável pelo dependente; null para USER.", nullable = true)
        String responsibleName,

        @Schema(description = "Data e hora da última atualização.", format = "date-time")
        Instant updatedAt
) {
    public static MedicalProfileSummaryResponse fromPresentation(AdminMedicalProfilePresentation presentation) {
        var profile = presentation.profile();
        return new MedicalProfileSummaryResponse(
                profile.getId(),
                profile.getOwnerType(),
                profile.getOwnerId(),
                presentation.names().ownerName(),
                presentation.names().responsibleName(),
                profile.getUpdatedAt()
        );
    }
    @Override
    public String toString() {
        return "MedicalProfileSummaryResponse[id=" + id + ", ownerType=" + ownerType + ", ownerId=" + ownerId + ", names=REDACTED]";
    }
}
