package com.jeepclub.backend.health.core.application.query;

import com.jeepclub.backend.health.api.module.medicalprofile.MedicalProfileAccess;
import com.jeepclub.backend.health.api.module.medicalprofile.MedicalProfileBloodType;
import com.jeepclub.backend.health.api.module.medicalprofile.MedicalProfileCommand;
import com.jeepclub.backend.health.api.module.medicalprofile.MedicalProfileOwner;
import com.jeepclub.backend.health.api.module.medicalprofile.MedicalProfileView;
import com.jeepclub.backend.health.core.application.MedicalProfileData;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfileNotFoundException;
import com.jeepclub.backend.health.core.application.service.medicalprofile.internal.MedicalProfileManager;
import com.jeepclub.backend.health.core.domain.enums.BloodType;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.domain.model.MedicalProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
class HealthMedicalProfileAccessService implements MedicalProfileAccess {

    private final MedicalProfileManager medicalProfileManager;

    @Override
    @Transactional(readOnly = true)
    public Optional<MedicalProfileView> findByOwner(
            MedicalProfileOwner ownerType,
            Long ownerId
    ) {
        try {
            return Optional.of(toView(
                    medicalProfileManager.getByOwner(toDomain(ownerType), ownerId)
            ));
        } catch (MedicalProfileNotFoundException exception) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public MedicalProfileView upsertByOwner(
            MedicalProfileOwner ownerType,
            Long ownerId,
            MedicalProfileCommand command
    ) {
        return toView(medicalProfileManager.upsertByOwner(
                toDomain(ownerType),
                ownerId,
                toApplicationData(command)
        ));
    }

    private MedicalProfileData toApplicationData(MedicalProfileCommand command) {
        return new MedicalProfileData(
                command.bloodType() == null ? null : BloodType.valueOf(command.bloodType().name()),
                command.allergies(),
                command.chronicConditions(),
                command.continuousMedications(),
                command.healthInsuranceProvider(),
                command.healthInsurancePlan(),
                command.healthInsuranceNumber(),
                command.emergencyContactName(),
                command.emergencyContactPhone(),
                command.emergencyContactRelationship(),
                command.observations()
        );
    }

    private MedicalProfileOwnerType toDomain(MedicalProfileOwner ownerType) {
        return ownerType == null ? null : MedicalProfileOwnerType.valueOf(ownerType.name());
    }

    private MedicalProfileView toView(MedicalProfile profile) {
        return new MedicalProfileView(
                profile.getId(),
                MedicalProfileOwner.valueOf(profile.getOwnerType().name()),
                profile.getOwnerId(),
                MedicalProfileBloodType.valueOf(profile.getBloodType().name()),
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
