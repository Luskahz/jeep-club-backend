package com.jeepclub.backend.health.core.application.service.medicalprofile;

import com.jeepclub.backend.health.core.application.command.UpsertMedicalProfileCommand;
import com.jeepclub.backend.health.core.application.exceptions.InvalidMedicalProfileDataException;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfileAccessDeniedException;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfileNotFoundException;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfileOwnerInactiveException;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfileOwnerNotFoundException;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.domain.model.MedicalProfile;
import com.jeepclub.backend.health.core.port.DependentOwnershipChecker;
import com.jeepclub.backend.health.core.port.MedicalProfileOwnerStatus;
import com.jeepclub.backend.health.core.port.MedicalProfileOwnerStatusChecker;
import com.jeepclub.backend.health.core.repository.MedicalProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class MedicalProfileService {

    private final MedicalProfileRepository medicalProfileRepository;
    private final DependentOwnershipChecker dependentOwnershipChecker;
    private final MedicalProfileOwnerStatusChecker ownerStatusChecker;
    private final Clock clock;

    @Transactional(readOnly = true)
    public MedicalProfile getMyMedicalProfile(Long userId) {
        validateAccessibleOwner(MedicalProfileOwnerType.USER, userId);

        return findByOwner(
                MedicalProfileOwnerType.USER,
                userId
        );
    }

    @Transactional
    public MedicalProfile upsertMyMedicalProfile(
            Long userId,
            UpsertMedicalProfileCommand data
    ) {
        validateAccessibleOwner(MedicalProfileOwnerType.USER, userId);

        return upsertByOwner(
                MedicalProfileOwnerType.USER,
                userId,
                data
        );
    }

    @Transactional(readOnly = true)
    public MedicalProfile getDependentMedicalProfile(
            Long userId,
            Long dependentId
    ) {
        validateDependentBelongsToUser(
                dependentId,
                userId
        );

        return findByOwner(
                MedicalProfileOwnerType.DEPENDENT,
                dependentId
        );
    }

    @Transactional
    public MedicalProfile upsertDependentMedicalProfile(
            Long userId,
            Long dependentId,
            UpsertMedicalProfileCommand data
    ) {
        validateDependentBelongsToUser(
                dependentId,
                userId
        );

        return upsertByOwner(
                MedicalProfileOwnerType.DEPENDENT,
                dependentId,
                data
        );
    }

    @Transactional
    public void deleteMyMedicalProfile(Long userId) {
        validateAccessibleOwner(MedicalProfileOwnerType.USER, userId);

        MedicalProfile profile = findByOwner(
                MedicalProfileOwnerType.USER,
                userId
        );

        medicalProfileRepository.delete(
                profile,
                userId,
                Instant.now(clock)
        );
    }

    @Transactional
    public void deleteDependentMedicalProfile(
            Long userId,
            Long dependentId
    ) {
        validateDependentBelongsToUser(
                dependentId,
                userId
        );

        MedicalProfile profile = findByOwner(
                MedicalProfileOwnerType.DEPENDENT,
                dependentId
        );

        medicalProfileRepository.delete(
                profile,
                userId,
                Instant.now(clock)
        );
    }

    private MedicalProfile findByOwner(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    ) {
        return medicalProfileRepository
                .findByOwner(ownerType, ownerId)
                .orElseThrow(MedicalProfileNotFoundException::new);
    }

    private MedicalProfile upsertByOwner(
            MedicalProfileOwnerType ownerType,
            Long ownerId,
            UpsertMedicalProfileCommand data
    ) {
        var existing = medicalProfileRepository.findByOwnerForUpdate(
                ownerType,
                ownerId
        );
        if (existing.isPresent()) {
            return updateProfile(existing.get(), data);
        }
        return createProfile(ownerType, ownerId, data);
    }

    private MedicalProfile updateProfile(
            MedicalProfile profile,
            UpsertMedicalProfileCommand data
    ) {
        profile.update(
                data.bloodType(),
                data.allergies(),
                data.chronicConditions(),
                data.continuousMedications(),
                data.healthInsuranceProvider(),
                data.healthInsurancePlan(),
                data.healthInsuranceNumber(),
                data.emergencyContactName(),
                data.emergencyContactPhone(),
                data.emergencyContactRelationship(),
                data.observations(),
                Instant.now(clock)
        );

        return medicalProfileRepository.save(profile);
    }

    private MedicalProfile createProfile(
            MedicalProfileOwnerType ownerType,
            Long ownerId,
            UpsertMedicalProfileCommand data
    ) {
        MedicalProfile profile = MedicalProfile.create(
                ownerType,
                ownerId,
                data.bloodType(),
                data.allergies(),
                data.chronicConditions(),
                data.continuousMedications(),
                data.healthInsuranceProvider(),
                data.healthInsurancePlan(),
                data.healthInsuranceNumber(),
                data.emergencyContactName(),
                data.emergencyContactPhone(),
                data.emergencyContactRelationship(),
                data.observations(),
                Instant.now(clock)
        );

        return medicalProfileRepository.save(profile);
    }

    private void validateDependentBelongsToUser(
            Long dependentId,
            Long userId
    ) {
        validateAccessibleOwner(MedicalProfileOwnerType.USER, userId);
        validateAccessibleOwner(MedicalProfileOwnerType.DEPENDENT, dependentId);

        if (!dependentOwnershipChecker.belongsToUser(
                dependentId,
                userId
        )) {
            throw new MedicalProfileAccessDeniedException(
                    "O dependente informado não pertence ao usuário autenticado."
            );
        }
    }

    private void validateAccessibleOwner(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    ) {
        if (ownerId == null || ownerId <= 0) {
            throw new InvalidMedicalProfileDataException(
                    "O ID do proprietário do perfil médico deve ser positivo."
            );
        }

        MedicalProfileOwnerStatus status = ownerStatusChecker.getStatus(
                ownerType,
                ownerId
        );

        if (status == MedicalProfileOwnerStatus.NOT_FOUND) {
            throw new MedicalProfileOwnerNotFoundException(ownerType, ownerId);
        }

        if (status == MedicalProfileOwnerStatus.INACTIVE) {
            throw new MedicalProfileOwnerInactiveException(ownerType, ownerId);
        }
    }

}
