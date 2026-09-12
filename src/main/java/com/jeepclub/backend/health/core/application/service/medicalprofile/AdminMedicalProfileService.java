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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminMedicalProfileService {

    private static final int OWNER_LOOKUP_BATCH_SIZE = 100;

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
        List<MedicalProfile> content = new ArrayList<>(pageable.getPageSize());
        long eligibleCount = 0;
        int sourcePageNumber = 0;
        Page<MedicalProfile> sourcePage;

        do {
            sourcePage = medicalProfileRepository.findAll(PageRequest.of(
                    sourcePageNumber++,
                    OWNER_LOOKUP_BATCH_SIZE,
                    pageable.getSort()
            ));

            Set<Long> activeUserIds = ownerStatusChecker.findActiveOwnerIds(
                    MedicalProfileOwnerType.USER,
                    ownerIds(sourcePage.getContent(), MedicalProfileOwnerType.USER)
            );
            Set<Long> activeDependentIds = ownerStatusChecker.findActiveOwnerIds(
                    MedicalProfileOwnerType.DEPENDENT,
                    ownerIds(sourcePage.getContent(), MedicalProfileOwnerType.DEPENDENT)
            );

            for (MedicalProfile profile : sourcePage) {
                if (isEligible(profile, activeUserIds, activeDependentIds)) {
                    if (eligibleCount >= pageable.getOffset()
                            && content.size() < pageable.getPageSize()) {
                        content.add(profile);
                    }
                    eligibleCount++;
                }
            }
        } while (sourcePage.hasNext());

        return new PageImpl<>(content, pageable, eligibleCount);
    }

    private Set<Long> ownerIds(
            List<MedicalProfile> profiles,
            MedicalProfileOwnerType ownerType
    ) {
        return profiles.stream()
                .filter(profile -> profile.getOwnerType() == ownerType)
                .map(MedicalProfile::getOwnerId)
                .collect(java.util.stream.Collectors.toSet());
    }

    private boolean isEligible(
            MedicalProfile profile,
            Set<Long> activeUserIds,
            Set<Long> activeDependentIds
    ) {
        return switch (profile.getOwnerType()) {
            case USER -> activeUserIds.contains(profile.getOwnerId());
            case DEPENDENT -> activeDependentIds.contains(profile.getOwnerId());
        };
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
