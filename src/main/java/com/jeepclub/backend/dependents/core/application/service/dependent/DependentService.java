package com.jeepclub.backend.dependents.core.application.service.dependent;

import com.jeepclub.backend.dependents.core.application.result.DependentResult;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import com.jeepclub.backend.dependents.core.domain.exception.DependentException;
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
            boolean consentAccepted,
            DependentMedicalProfileData medicalProfile,
            Long socioId
    ) {
        Instant now = Instant.now(clock);

        if (!dependentUserPort.existsById(socioId)) {
            throw DependentException.notFound();
        }

        String cleanCpf = normalizeCpf(cpf);
        if (cleanCpf != null
                && (dependentUserPort.existsByCpf(cleanCpf)
                || dependentRepository.existsByCpf(cleanCpf))) {
            throw DependentException.conflict();
        }

        Dependent dependent = Dependent.create(
                name,
                cleanCpf,
                birthDate,
                relationshipType,
                phoneNumber,
                consentAccepted,
                socioId,
                now
        );

        Dependent savedDependent = dependentRepository.save(dependent);
        upsertMedicalProfileIfPresent(savedDependent.getId(), medicalProfile);
        return toResult(savedDependent);
    }

    @Transactional(readOnly = true)
    public List<DependentResult> findAllBySocioId(Long socioId) {
        return dependentRepository.findAllBySocioId(socioId).stream()
                .map(this::toResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public DependentResult findById(Long id, Long requestingUserId) {
        Dependent dependent = findDependentById(id);
        assertBelongsTo(dependent, requestingUserId);
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
            boolean consentAccepted,
            DependentMedicalProfileData medicalProfile,
            Long requestingUserId
    ) {
        Dependent dependent = findDependentById(id);
        assertBelongsTo(dependent, requestingUserId);

        String cleanCpf = normalizeCpf(cpf);
        if (cleanCpf != null && !cleanCpf.equals(dependent.getCpf())) {
            if (dependentUserPort.existsByCpf(cleanCpf)
                    || dependentRepository.existsByCpfAndIdNot(cleanCpf, id)) {
                throw DependentException.conflict();
            }
        }

        dependent.update(
                name,
                cleanCpf,
                birthDate,
                relationshipType,
                phoneNumber,
                consentAccepted,
                Instant.now(clock)
        );

        Dependent savedDependent = dependentRepository.save(dependent);
        upsertMedicalProfileIfPresent(savedDependent.getId(), medicalProfile);
        return toResult(savedDependent);
    }

    @Transactional
    public void delete(Long id, Long requestingUserId) {
        Dependent dependent = findDependentById(id);
        assertBelongsTo(dependent, requestingUserId);
        dependentRepository.deleteById(id);
    }

    private Dependent findDependentById(Long id) {
        return dependentRepository.findById(id)
                .orElseThrow(DependentException::notFound);
    }

    private void assertBelongsTo(Dependent dependent, Long requestingUserId) {
        if (!dependent.getSocioId().equals(requestingUserId)) {
            throw DependentException.accessDenied();
        }
    }

    private void upsertMedicalProfileIfPresent(
            Long dependentId,
            DependentMedicalProfileData medicalProfile
    ) {
        if (medicalProfile != null && medicalProfile.hasAnyValue()) {
            medicalProfilePort.upsert(dependentId, medicalProfile);
        }
    }

    private DependentResult toResult(Dependent dependent) {
        return new DependentResult(
                dependent,
                medicalProfilePort.findByDependentId(dependent.getId())
        );
    }

    private String normalizeCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return null;
        }
        return cpf.replaceAll("\\D", "");
    }
}
