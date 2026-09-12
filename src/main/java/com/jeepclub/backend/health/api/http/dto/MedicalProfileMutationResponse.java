package com.jeepclub.backend.health.api.http.dto;

import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.domain.model.MedicalProfile;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Confirmação administrativa sem reexposição de dados clínicos.")
public record MedicalProfileMutationResponse(
        Long id,
        MedicalProfileOwnerType ownerType,
        Long ownerId,
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
