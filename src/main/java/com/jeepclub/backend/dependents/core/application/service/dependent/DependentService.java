package com.jeepclub.backend.dependents.core.application.service.dependent;

import com.jeepclub.backend.dependents.core.application.exception.*;
import com.jeepclub.backend.dependents.core.application.result.DependentResult;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import com.jeepclub.backend.dependents.core.domain.model.Dependent;
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
    private final Clock clock;

    @Transactional
    public DependentResult create(
            String name,
            String cpf,
            LocalDate birthDate,
            RelationshipType relationshipType,
            String phoneNumber,
            Long userId
    ) {
        assertUserCanOwnDependent(userId);

        String normalizedCpf = normalizeCpf(cpf);

        assertCpfAvailable(normalizedCpf);

        Dependent dependent = Dependent.create(
                name,
                normalizedCpf,
                birthDate,
                relationshipType,
                phoneNumber,
                userId,
                Instant.now(clock)
        );

        return toResult(
                dependentRepository.save(dependent)
        );
    }

    @Transactional(readOnly = true)
    public List<DependentResult> findAllByUserId(Long userId) {
        return dependentRepository.findAllActiveByUserId(userId)
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

        return toResult(
                dependentRepository.save(dependent)
        );
    }

    @Transactional
    public void delete(
            Long id,
            Long requestingUserId
    ) {
        Dependent dependent = findDependentById(id);

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

    private void assertUserCanOwnDependent(Long userId) {
        if (!dependentUserPort.existsById(userId)) {
            throw new DependentOwnerNotFoundException(userId);
        }

        if (!dependentUserPort.existsActiveById(userId)) {
            throw new DependentOwnerInactiveException(userId);
        }
    }

    private void assertBelongsTo(
            Dependent dependent,
            Long requestingUserId
    ) {
        if (!dependent.getUserId().equals(requestingUserId)) {
            throw new DependentAccessDeniedException(
                    dependent.getId()
            );
        }
    }

    private void assertCpfAvailable(String cpf) {
        if (dependentUserPort.existsByCpf(cpf)
                || dependentRepository.existsActiveByCpf(cpf)) {

            throw new DependentCpfAlreadyInUseException();
        }
    }

    private void assertCpfAvailableForUpdate(
            String cpf,
            Long dependentId
    ) {
        if (dependentUserPort.existsByCpf(cpf)
                || dependentRepository.existsActiveByCpfAndIdNot(
                cpf,
                dependentId
        )) {

            throw new DependentCpfAlreadyInUseException();
        }
    }

    private DependentResult toResult(Dependent dependent) {
        return new DependentResult(dependent);
    }

    private String normalizeCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            throw new IllegalArgumentException(
                    "cpf is required"
            );
        }

        return cpf.replaceAll("\\D", "");
    }

    private Dependent findDependentById(Long id) {
        return dependentRepository.findById(id)
                .orElseThrow(
                        () -> new DependentNotFoundException(id)
                );
    }
}
