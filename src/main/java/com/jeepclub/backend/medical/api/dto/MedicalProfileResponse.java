package com.jeepclub.backend.medical.api.dto;

import com.jeepclub.backend.medical.core.domain.BloodType;
import com.jeepclub.backend.medical.core.domain.MedicalProfile;
import com.jeepclub.backend.medical.core.domain.MedicalProfileOwnerType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Resposta detalhada com os dados do perfil médico.")
public record MedicalProfileResponse(
        @Schema(description = "ID do perfil médico.", example = "1")
        Long id,

        @Schema(description = "Tipo do proprietário do perfil médico.", example = "USER")
        MedicalProfileOwnerType ownerType,

        @Schema(description = "ID do usuário ou dependente proprietário do perfil médico.", example = "10")
        Long ownerId,

        @Schema(description = "Tipo sanguíneo.", example = "O_POSITIVE")
        BloodType bloodType,

        @Schema(description = "Alergias conhecidas.")
        String allergies,

        @Schema(description = "Doenças ou condições crônicas relevantes.")
        String chronicConditions,

        @Schema(description = "Medicamentos de uso contínuo.")
        String continuousMedications,

        @Schema(description = "Operadora do convênio médico.")
        String healthInsuranceProvider,

        @Schema(description = "Plano do convênio médico.")
        String healthInsurancePlan,

        @Schema(description = "Número da carteirinha ou identificação do convênio.")
        String healthInsuranceNumber,

        @Schema(description = "Nome do contato de emergência.")
        String emergencyContactName,

        @Schema(description = "Telefone do contato de emergência.")
        String emergencyContactPhone,

        @Schema(description = "Relação do contato de emergência com o usuário ou dependente.")
        String emergencyContactRelationship,

        @Schema(description = "Observações médicas gerais.")
        String observations,

        @Schema(description = "Data e hora de criação do perfil médico.")
        Instant createdAt,

        @Schema(description = "Data e hora da última atualização do perfil médico.")
        Instant updatedAt
) {
    public static MedicalProfileResponse fromDomain(MedicalProfile profile) {
        return new MedicalProfileResponse(
                profile.getId(),
                profile.getOwnerType(),
                profile.getOwnerId(),
                profile.getBloodType(),
                profile.getAllergies(),
                profile.getChronicConditions(),
                profile.getContinuousMedications(),
                profile.getHealthInsuranceProvider(),
                profile.getHealthInsurancePlan(),
                profile.getHealthInsuranceNumber(),
                profile.getEmergencyContactName(),
                profile.getEmergencyContactPhone(),
                profile.getEmergencyContactRelationship(),
                profile.getObservations(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
