package com.jeepclub.backend.health.core.application;

import com.jeepclub.backend.health.api.http.dto.MedicalProfileRequest;
import com.jeepclub.backend.health.core.application.exceptions.DependentOwnershipValidationUnavailableException;
import com.jeepclub.backend.health.core.application.exceptions.InvalidMedicalProfileDataException;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfileAccessDeniedException;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfileNotFoundException;
import com.jeepclub.backend.health.core.ports.DependentOwnershipChecker;
import com.jeepclub.backend.health.core.domain.MedicalProfile;
import com.jeepclub.backend.health.core.domain.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.repository.MedicalProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MedicalProfileService {

    private static final int MAX_PAGE_SIZE = 100;

    private final MedicalProfileRepository medicalProfileRepository;
    private final Optional<DependentOwnershipChecker> dependentOwnershipChecker;
    private final Clock clock;

    @Transactional(readOnly = true)
    public MedicalProfile getMyMedicalProfile(Long userId) {
        return getByOwner(MedicalProfileOwnerType.USER, userId);
    }

    @Transactional
    public MedicalProfile upsertMyMedicalProfile(
            Long userId,
            MedicalProfileRequest request
    ) {
        return upsertByOwner(MedicalProfileOwnerType.USER, userId, request);
    }

    @Transactional(readOnly = true)
    public MedicalProfile getDependentMedicalProfile(
            Long userId,
            Long dependentId
    ) {
        validateDependentBelongsToUser(dependentId, userId);
        return getByOwner(MedicalProfileOwnerType.DEPENDENT, dependentId);
    }

    @Transactional
    public MedicalProfile upsertDependentMedicalProfile(
            Long userId,
            Long dependentId,
            MedicalProfileRequest request
    ) {
        validateDependentBelongsToUser(dependentId, userId);
        return upsertByOwner(MedicalProfileOwnerType.DEPENDENT, dependentId, request);
    }

    @Transactional(readOnly = true)
    public MedicalProfile getById(Long id) {
        if (id == null) {
            throw new InvalidMedicalProfileDataException("O ID do perfil médico é obrigatório.");
        }

        return medicalProfileRepository
                .findById(id)
                .orElseThrow(MedicalProfileNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public MedicalProfile getByOwner(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    ) {
        validateOwner(ownerType, ownerId);

        return medicalProfileRepository
                .findByOwner(ownerType, ownerId)
                .orElseThrow(MedicalProfileNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public List<MedicalProfile> listMedicalProfiles(
            int page,
            int size
    ) {
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = sanitizePageSize(size);

        return medicalProfileRepository.findAll(sanitizedPage, sanitizedSize);
    }

    @Transactional
    public MedicalProfile upsertByOwner(
            MedicalProfileOwnerType ownerType,
            Long ownerId,
            MedicalProfileRequest request
    ) {
        validateOwner(ownerType, ownerId);

        return medicalProfileRepository
                .findByOwner(ownerType, ownerId)
                .map(existing -> updateExisting(existing, request))
                .orElseGet(() -> createNew(ownerType, ownerId, request));
    }

    private MedicalProfile updateExisting(
            MedicalProfile existing,
            MedicalProfileRequest request
    ) {
        Instant now = Instant.now(clock);
        existing.update(
                request.bloodType(),
                clean(request.allergies()),
                clean(request.chronicConditions()),
                clean(request.continuousMedications()),
                clean(request.healthInsuranceProvider()),
                clean(request.healthInsurancePlan()),
                clean(request.healthInsuranceNumber()),
                clean(request.emergencyContactName()),
                normalizePhone(request.emergencyContactPhone()),
                clean(request.emergencyContactRelationship()),
                clean(request.observations()),
                now
        );

        return medicalProfileRepository.save(existing);
    }

    private MedicalProfile createNew(
            MedicalProfileOwnerType ownerType,
            Long ownerId,
            MedicalProfileRequest request
    ) {
        Instant now = Instant.now(clock);

        var profile = MedicalProfile.create(
                ownerType,
                ownerId,
                request.bloodType(),
                clean(request.allergies()),
                clean(request.chronicConditions()),
                clean(request.continuousMedications()),
                clean(request.healthInsuranceProvider()),
                clean(request.healthInsurancePlan()),
                clean(request.healthInsuranceNumber()),
                clean(request.emergencyContactName()),
                normalizePhone(request.emergencyContactPhone()),
                clean(request.emergencyContactRelationship()),
                clean(request.observations()),
                now
        );

        return medicalProfileRepository.save(profile);
    }

    private void validateOwner(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    ) {
        if (ownerType == null) {
            throw new InvalidMedicalProfileDataException("O tipo do proprietário do perfil médico é obrigatório.");
        }

        if (ownerId == null) {
            throw new InvalidMedicalProfileDataException("O ID do proprietário do perfil médico é obrigatório.");
        }
    }

    private void validateDependentBelongsToUser(
            Long dependentId,
            Long userId
    ) {
        if (dependentId == null) {
            throw new InvalidMedicalProfileDataException("O ID do dependente é obrigatório.");
        }

        if (userId == null) {
            throw new InvalidMedicalProfileDataException("O ID do usuário autenticado é obrigatório.");
        }

        var checker = dependentOwnershipChecker
                .orElseThrow(DependentOwnershipValidationUnavailableException::new);

        boolean belongsToUser = checker.belongsToUser(dependentId, userId);

        if (!belongsToUser) {
            throw new MedicalProfileAccessDeniedException(
                    "O dependente informado não pertence ao usuário autenticado."
            );
        }
    }

    private int sanitizePageSize(int size) {
        if (size <= 0) {
            return 20;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value.trim();
        return cleaned.isBlank() ? null : cleaned;
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }

        String digits = phone.replaceAll("\\D", "");

        if (digits.isBlank()) {
            return null;
        }

        if (digits.length() < 10 || digits.length() > 11) {
            throw new InvalidMedicalProfileDataException(
                    "O telefone de emergência deve ter 10 ou 11 dígitos."
            );
        }

        return digits;
    }
}
