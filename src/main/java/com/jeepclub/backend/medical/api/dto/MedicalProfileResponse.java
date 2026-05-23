package com.jeepclub.backend.medical.api.dto;

import com.jeepclub.backend.medical.core.domain.BloodType;
import com.jeepclub.backend.medical.core.domain.MedicalProfile;
import com.jeepclub.backend.medical.core.domain.MedicalProfileOwnerType;

import java.time.Instant;

// cuidado com oque vc exporta numa dto aqui está ok, mas se precisar exportar futuramente uma lista de medicalProfiles isso fica mt grande.
// como é uma rota que demanda id, sabemos que o frontend só vai conseguir consultar uma, mas quando for realizar uma rota de read adm atenção nisso;

// pendente documentação swagger na sua dto, olha as dtos do modulo authorization. estão todas no padrão deixe seu claude se inspirar kkk

public record MedicalProfileResponse(
        Long id,
        MedicalProfileOwnerType ownerType,
        Long ownerId,
        BloodType bloodType,
        String allergies,
        String chronicConditions,
        String continuousMedications,
        String healthInsuranceProvider,
        String healthInsurancePlan,
        String healthInsuranceNumber,
        String emergencyContactName,
        String emergencyContactPhone,
        String emergencyContactRelationship,
        String observations,
        Instant createdAt,
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