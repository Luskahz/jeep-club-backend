package com.jeepclub.backend.dependents.core.application.service.dependent;

import com.jeepclub.backend.dependents.core.application.exception.DependentAccessDeniedException;
import com.jeepclub.backend.dependents.core.application.exception.DependentCpfAlreadyInUseException;
import com.jeepclub.backend.dependents.core.application.exception.DependentNotFoundException;
import com.jeepclub.backend.dependents.core.application.exception.SocioNotFoundException;
import com.jeepclub.backend.dependents.core.application.result.DependentResult;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import com.jeepclub.backend.dependents.core.domain.model.Dependent;
import com.jeepclub.backend.dependents.core.port.DependentMedicalProfileData;
import com.jeepclub.backend.dependents.core.port.DependentMedicalProfilePort;
import com.jeepclub.backend.dependents.core.port.DependentUserPort;
import com.jeepclub.backend.dependents.core.repository.DependentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DependentService {

    private final DependentRepository dependentRepository;
    private final DependentUserPort dependentUserPort;
    private final DependentMedicalProfilePort medicalProfilePort;
    private final Clock clock;

    @Transactional
    public DependentResult create(
            String name,
            String cpf,
            LocalDate birthDate,
            RelationshipType relationshipType,
            String phoneNumber,
            DependentMedicalProfileData medicalProfile,
            Long socioId
    ) {
        assertSocioExists(socioId);

        String normalizedCpf = normalizeCpf(cpf);

        assertCpfAvailable(normalizedCpf);

        Instant now = Instant.now(clock);

        Dependent dependent = Dependent.create(
                name,
                normalizedCpf,
                birthDate,
                relationshipType,
                phoneNumber,
                socioId,
                now
        );

        Dependent savedDependent = dependentRepository.save(dependent);

        upsertMedicalProfileIfPresent(
                savedDependent.getId(),
                medicalProfile
        );

        return toResult(savedDependent);
    }

    @Transactional(readOnly = true)
    public List<DependentResult> findAllBySocioId(Long socioId) {
        return dependentRepository.findAllActiveBySocioId(socioId)
                .stream()
                .map(this::toResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public DependentResult findById(
            Long id,
            Long requestingUserId
    ) {
        Dependent dependent = findActiveDependentById(id);

        assertBelongsTo(
                dependent,
                requestingUserId
        );

        return toResult(dependent);
    }

    @Transactional
    public DependentResult update(
            Long id,
            String name,
            String cpf,
            LocalDate birthDate,
            RelationshipType relationshipType,
            String phoneNumber,
            DependentMedicalProfileData medicalProfile,
            Long requestingUserId
    ) {
        Dependent dependent = findActiveDependentById(id);

        assertBelongsTo(
                dependent,
                requestingUserId
        );

        String normalizedCpf = normalizeCpf(cpf);

        if (!normalizedCpf.equals(dependent.getCpf())) {
            assertCpfAvailableForUpdate(
                    normalizedCpf,
                    dependent.getId()
            );
        }

        dependent.update(
                name,
                normalizedCpf,
                birthDate,
                relationshipType,
                phoneNumber,
                Instant.now(clock)
        );

        Dependent savedDependent = dependentRepository.save(dependent);

        upsertMedicalProfileIfPresent(
                savedDependent.getId(),
                medicalProfile
        );

        return toResult(savedDependent);
    }

    @Transactional
    public void delete(
            Long id,
            Long requestingUserId
    ) {
        Dependent dependent = findActiveDependentById(id);

        assertBelongsTo(
                dependent,
                requestingUserId
        );

        dependent.selfDelete(
                Instant.now(clock)
        );

        dependentRepository.save(dependent);
    }

    private Dependent findActiveDependentById(Long id) {
        return dependentRepository.findActiveById(id)
                .orElseThrow(
                        () -> new DependentNotFoundException(id)
                );
    }

    private void assertSocioExists(Long socioId) {
        if (!dependentUserPort.existsById(socioId)) {
            throw new SocioNotFoundException(socioId);
        }
    }

    private void assertBelongsTo(
            Dependent dependent,
            Long requestingUserId
    ) {
        if (!dependent.getSocioId().equals(requestingUserId)) {
            throw new DependentAccessDeniedException(
                    dependent.getId()
            );
        }
    }

    private void assertCpfAvailable(String cpf) {
        boolean userAlreadyUsesCpf =
                dependentUserPort.existsByCpf(cpf);

        boolean activeDependentAlreadyUsesCpf =
                dependentRepository.existsActiveByCpf(cpf);

        if (userAlreadyUsesCpf || activeDependentAlreadyUsesCpf) {
            throw new DependentCpfAlreadyInUseException(cpf);
        }
    }

    private void assertCpfAvailableForUpdate(
            String cpf,
            Long dependentId
    ) {
        boolean userAlreadyUsesCpf =
                dependentUserPort.existsByCpf(cpf);

        boolean anotherActiveDependentUsesCpf =
                dependentRepository.existsActiveByCpfAndIdNot(
                        cpf,
                        dependentId
                );

        if (userAlreadyUsesCpf || anotherActiveDependentUsesCpf) {
            throw new DependentCpfAlreadyInUseException(cpf);
        }
    }

    private void upsertMedicalProfileIfPresent(
            Long dependentId,
            DependentMedicalProfileData medicalProfile
    ) {
        if (medicalProfile == null
                || !medicalProfile.hasAnyValue()) {
            return;
        }

        medicalProfilePort.upsert(
                dependentId,
                medicalProfile
        );
    }

    private DependentResult toResult(
            Dependent dependent
    ) {
        return new DependentResult(
                dependent,
                medicalProfilePort.findByDependentId(
                        dependent.getId()
                )
        );
    }

    private String normalizeCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            throw new IllegalArgumentException(
                    "cpf is required"
            );
        }

        return cpf.replaceAll("\\D", "");
    }
}