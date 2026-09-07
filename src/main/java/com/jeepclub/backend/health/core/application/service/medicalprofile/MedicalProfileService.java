package com.jeepclub.backend.health.core.application.service.medicalprofile;

import com.jeepclub.backend.health.core.application.command.UpsertMedicalProfileCommand;
import com.jeepclub.backend.health.core.application.audit.MedicalProfileAuditEvent;
import com.jeepclub.backend.health.core.application.audit.MedicalProfileAuditOperation;
import com.jeepclub.backend.health.core.application.audit.MedicalProfileAuditOutcome;
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
import com.jeepclub.backend.health.core.port.MedicalProfileAuditTrail;
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
    private final MedicalProfileAuditTrail auditTrail;
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
                data,
                userId
        );
    }

    @Transactional(readOnly = true)
    public MedicalProfile getDependentMedicalProfile(
            Long userId,
            Long dependentId
    ) {
        validateDependentBelongsToUser(
                dependentId,
                userId,
                MedicalProfileAuditOperation.READ
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
                userId,
                MedicalProfileAuditOperation.UPDATE
        );

        return upsertByOwner(
                MedicalProfileOwnerType.DEPENDENT,
                dependentId,
                data,
                userId
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
        audit(userId, profile, MedicalProfileAuditOperation.DELETE,
                MedicalProfileAuditOutcome.SUCCEEDED);
    }

    @Transactional
    public void deleteDependentMedicalProfile(
            Long userId,
            Long dependentId
    ) {
        validateDependentBelongsToUser(
                dependentId,
                userId,
                MedicalProfileAuditOperation.DELETE
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
        audit(userId, profile, MedicalProfileAuditOperation.DELETE,
                MedicalProfileAuditOutcome.SUCCEEDED);
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
            UpsertMedicalProfileCommand data,
            Long actorUserId
    ) {
        var existing = medicalProfileRepository.findByOwnerForUpdate(
                ownerType,
                ownerId
        );
        MedicalProfile profile;
        MedicalProfileAuditOperation operation;

        if (existing.isPresent()) {
            profile = updateProfile(existing.get(), data);
            operation = MedicalProfileAuditOperation.UPDATE;
        } else {
            profile = createProfile(ownerType, ownerId, data);
            operation = MedicalProfileAuditOperation.CREATE;
        }

        audit(actorUserId, profile, operation, MedicalProfileAuditOutcome.SUCCEEDED);
        return profile;
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
            Long userId,
            MedicalProfileAuditOperation operation
    ) {
        validateAccessibleOwner(MedicalProfileOwnerType.USER, userId);
        validateAccessibleOwner(MedicalProfileOwnerType.DEPENDENT, dependentId);

        if (!dependentOwnershipChecker.belongsToUser(
                dependentId,
                userId
        )) {
            auditTrail.record(new MedicalProfileAuditEvent(
                    userId,
                    MedicalProfileOwnerType.DEPENDENT,
                    dependentId,
                    operation,
                    MedicalProfileAuditOutcome.DENIED,
                    Instant.now(clock)
            ));
            throw new MedicalProfileAccessDeniedException(
                    "O dependente informado não pertence ao usuário autenticado."
            );
        }
    }

    private void audit(
            Long actorUserId,
            MedicalProfile profile,
            MedicalProfileAuditOperation operation,
            MedicalProfileAuditOutcome outcome
    ) {
        auditTrail.record(new MedicalProfileAuditEvent(
                actorUserId,
                profile.getOwnerType(),
                profile.getOwnerId(),
                operation,
                outcome,
                Instant.now(clock)
        ));
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
