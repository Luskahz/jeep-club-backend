package com.jeepclub.backend.dependents.infra.integration.health;

import com.jeepclub.backend.dependents.core.port.DependentMedicalProfileData;
import com.jeepclub.backend.dependents.core.port.DependentMedicalProfilePort;
import com.jeepclub.backend.health.api.module.medicalprofile.MedicalProfileAccess;
import com.jeepclub.backend.health.api.module.medicalprofile.MedicalProfileBloodType;
import com.jeepclub.backend.health.api.module.medicalprofile.MedicalProfileCommand;
import com.jeepclub.backend.health.api.module.medicalprofile.MedicalProfileOwner;
import com.jeepclub.backend.health.api.module.medicalprofile.MedicalProfileView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DependentMedicalProfileAdapter implements DependentMedicalProfilePort {

    private final MedicalProfileAccess medicalProfileAccess;

    @Override
    public DependentMedicalProfileData findByDependentId(Long dependentId) {
        return medicalProfileAccess.findByOwner(
                        MedicalProfileOwner.DEPENDENT,
                        dependentId
                )
                .map(this::toData)
                .orElse(null);
    }

    @Override
    public void upsert(Long dependentId, DependentMedicalProfileData medicalProfile) {
        medicalProfileAccess.upsertByOwner(
                MedicalProfileOwner.DEPENDENT,
                dependentId,
                new MedicalProfileCommand(
                        parseBloodType(medicalProfile.bloodType()),
                        medicalProfile.allergies(),
                        medicalProfile.chronicDiseases(),
                        medicalProfile.medications(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        medicalProfile.medicalNotes()
                )
        );
    }

    private DependentMedicalProfileData toData(MedicalProfileView profile) {
        return new DependentMedicalProfileData(
                profile.bloodType() == null ? null : profile.bloodType().name(),
                profile.allergies(),
                profile.chronicConditions(),
                profile.continuousMedications(),
                profile.observations()
        );
    }

    private MedicalProfileBloodType parseBloodType(String value) {
        if (value == null || value.isBlank()) {
            return MedicalProfileBloodType.UNKNOWN;
        }

        String normalized = value.trim()
                .toUpperCase()
                .replace("+", "_POSITIVE")
                .replace("-", "_NEGATIVE");

        return MedicalProfileBloodType.valueOf(normalized);
    }
}
