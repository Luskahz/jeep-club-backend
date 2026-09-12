package com.jeepclub.backend.health.core.application.service.medicalprofile;

import com.jeepclub.backend.health.core.application.command.UpsertMedicalProfileCommand;
import com.jeepclub.backend.health.core.application.exceptions.InvalidMedicalProfileDataException;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfileNotFoundException;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfileOwnerInactiveException;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfileOwnerNotFoundException;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.domain.model.MedicalProfile;
import com.jeepclub.backend.health.core.port.MedicalProfileOwnerStatus;
import com.jeepclub.backend.health.core.port.MedicalProfileOwnerStatusChecker;
import com.jeepclub.backend.health.core.repository.MedicalProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AdminMedicalProfileService {

    private final MedicalProfileRepository medicalProfileRepository;
    private final MedicalProfileOwnerStatusChecker ownerStatusChecker;
    private final Clock clock;

    @Transactional(readOnly = true)
    public MedicalProfile getById(Long id) {
        if (id == null || id <= 0) {
            throw new InvalidMedicalProfileDataException(
                    "O ID do perfil médico deve ser positivo."
            );
        }

        MedicalProfile profile = medicalProfileRepository
                .findById(id)
                .orElseThrow(MedicalProfileNotFoundException::new);
        validateAccessibleOwner(profile.getOwnerType(), profile.getOwnerId());
        return profile;
    }

    @Transactional(readOnly = true)
    public MedicalProfile getByOwner(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    ) {
        validateOwner(ownerType, ownerId);
        validateAccessibleOwner(ownerType, ownerId);

        MedicalProfile profile = medicalProfileRepository
                .findByOwner(ownerType, ownerId)
                .orElseThrow(MedicalProfileNotFoundException::new);
        return profile;
    }

    @Transactional(readOnly = true)
    public Page<MedicalProfile> listMedicalProfiles(
            Pageable pageable
    ) {
        return medicalProfileRepository.findAllWithActiveOwners(pageable);
    }

    @Transactional
    public MedicalProfile upsertByOwner(
            MedicalProfileOwnerType ownerType,
            Long ownerId,
            UpsertMedicalProfileCommand data
    ) {
        validateOwner(ownerType, ownerId);
        validateAccessibleOwner(ownerType, ownerId);

        var existing = medicalProfileRepository.findByOwnerForUpdate(
                ownerType,
                ownerId
        );
        if (existing.isPresent()) {
            return updateExisting(existing.get(), data);
        }
        return createNew(ownerType, ownerId, data);
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

}
