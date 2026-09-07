package com.jeepclub.backend.health.core.application.service.medicalprofile;

import com.jeepclub.backend.health.core.application.command.UpsertMedicalProfileCommand;
import com.jeepclub.backend.health.core.application.audit.MedicalProfileAuditEvent;
import com.jeepclub.backend.health.core.application.audit.MedicalProfileAuditOperation;
import com.jeepclub.backend.health.core.application.audit.MedicalProfileAuditOutcome;
import com.jeepclub.backend.health.core.application.exceptions.InvalidMedicalProfileDataException;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfileNotFoundException;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfileOwnerInactiveException;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfileOwnerNotFoundException;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.domain.model.MedicalProfile;
import com.jeepclub.backend.health.core.port.MedicalProfileOwnerStatus;
import com.jeepclub.backend.health.core.port.MedicalProfileOwnerStatusChecker;
import com.jeepclub.backend.health.core.port.MedicalProfileAuditTrail;
import com.jeepclub.backend.health.core.repository.MedicalProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminMedicalProfileService {

    private static final int MAX_PAGE_SIZE = 100;

    private final MedicalProfileRepository medicalProfileRepository;
    private final MedicalProfileOwnerStatusChecker ownerStatusChecker;
    private final MedicalProfileAuditTrail auditTrail;
    private final Clock clock;

    @Transactional(readOnly = true)
    public MedicalProfile getById(Long id, Long actorUserId) {
        validateActor(actorUserId);
        if (id == null || id <= 0) {
            throw new InvalidMedicalProfileDataException(
                    "O ID do perfil médico deve ser positivo."
            );
        }

        MedicalProfile profile = medicalProfileRepository
                .findById(id)
                .orElseThrow(MedicalProfileNotFoundException::new);
        validateAccessibleOwner(profile.getOwnerType(), profile.getOwnerId());
        audit(actorUserId, profile, MedicalProfileAuditOperation.READ);
        return profile;
    }

    @Transactional(readOnly = true)
    public MedicalProfile getByOwner(
            MedicalProfileOwnerType ownerType,
            Long ownerId,
            Long actorUserId
    ) {
        validateActor(actorUserId);
        validateOwner(ownerType, ownerId);
        validateAccessibleOwner(ownerType, ownerId);

        MedicalProfile profile = medicalProfileRepository
                .findByOwner(ownerType, ownerId)
                .orElseThrow(MedicalProfileNotFoundException::new);
        audit(actorUserId, profile, MedicalProfileAuditOperation.READ);
        return profile;
    }

    @Transactional(readOnly = true)
    public List<MedicalProfile> listMedicalProfiles(
            int page,
            int size,
            Long actorUserId
    ) {
        validateActor(actorUserId);
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = sanitizePageSize(size);

        List<MedicalProfile> profiles = medicalProfileRepository.findAll(
                sanitizedPage,
                sanitizedSize
        ).stream()
                .filter(this::hasActiveOwner)
                .toList();
        profiles.forEach(profile -> audit(
                actorUserId,
                profile,
                MedicalProfileAuditOperation.READ
        ));
        return profiles;
    }

    @Transactional
    public MedicalProfile upsertByOwner(
            MedicalProfileOwnerType ownerType,
            Long ownerId,
            UpsertMedicalProfileCommand data,
            Long actorUserId
    ) {
        validateActor(actorUserId);
        validateOwner(ownerType, ownerId);
        validateAccessibleOwner(ownerType, ownerId);

        var existing = medicalProfileRepository.findByOwnerForUpdate(
                ownerType,
                ownerId
        );
        MedicalProfile profile;
        MedicalProfileAuditOperation operation;

        if (existing.isPresent()) {
            profile = updateExisting(existing.get(), data);
            operation = MedicalProfileAuditOperation.UPDATE;
        } else {
            profile = createNew(ownerType, ownerId, data);
            operation = MedicalProfileAuditOperation.CREATE;
        }

        audit(actorUserId, profile, operation);
        return profile;
    }

    @Transactional
    public void deleteById(
            Long profileId,
            Long deletedByUserId
    ) {
        if (deletedByUserId == null || deletedByUserId <= 0) {
            throw new InvalidMedicalProfileDataException(
                    "O ID do usuário responsável pela exclusão deve ser positivo."
            );
        }

        if (profileId == null || profileId <= 0) {
            throw new InvalidMedicalProfileDataException(
                    "O ID do perfil médico deve ser positivo."
            );
        }

        // A limpeza administrativa permanece disponível mesmo quando o owner
        // foi desativado ou removido.
        MedicalProfile profile = medicalProfileRepository
                .findById(profileId)
                .orElseThrow(MedicalProfileNotFoundException::new);

        medicalProfileRepository.delete(
                profile,
                deletedByUserId,
                Instant.now(clock)
        );
        audit(deletedByUserId, profile, MedicalProfileAuditOperation.DELETE);
    }

    private MedicalProfile updateExisting(
            MedicalProfile existing,
            UpsertMedicalProfileCommand data
    ) {
        existing.update(
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

        return medicalProfileRepository.save(existing);
    }

    private MedicalProfile createNew(
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

    private void validateOwner(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    ) {
        if (ownerType == null) {
            throw new InvalidMedicalProfileDataException(
                    "O tipo do proprietário do perfil médico é obrigatório."
            );
        }

        if (ownerId == null || ownerId <= 0) {
            throw new InvalidMedicalProfileDataException(
                    "O ID do proprietário do perfil médico deve ser positivo."
            );
        }
    }

    private void validateAccessibleOwner(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    ) {
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

    private boolean hasActiveOwner(MedicalProfile profile) {
        return ownerStatusChecker.getStatus(
                profile.getOwnerType(),
                profile.getOwnerId()
        ) == MedicalProfileOwnerStatus.ACTIVE;
    }

    private void validateActor(Long actorUserId) {
        if (actorUserId == null || actorUserId <= 0) {
            throw new InvalidMedicalProfileDataException(
                    "O ID do ator da operação administrativa deve ser positivo."
            );
        }
    }

    private void audit(
            Long actorUserId,
            MedicalProfile profile,
            MedicalProfileAuditOperation operation
    ) {
        auditTrail.record(new MedicalProfileAuditEvent(
                actorUserId,
                profile.getOwnerType(),
                profile.getOwnerId(),
                operation,
                MedicalProfileAuditOutcome.SUCCEEDED,
                Instant.now(clock)
        ));
    }

    private int sanitizePageSize(int size) {
        if (size <= 0) {
            return 20;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }

}
