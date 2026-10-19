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
    private final com.jeepclub.backend.platform.logging.SystemLogService audit;
    private final java.util.List<com.jeepclub.backend.dependents.api.module.spi.DependentDeletionGuard> deletionGuards;

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
        Dependent dependent = findDependentById(id);

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
        Dependent dependent = findDependentForUpdate(id);

        assertBelongsTo(
                dependent,
                requestingUserId
        );

        if (!dependent.isActive()) throw new com.jeepclub.backend.dependents.core.domain.exception.DependentLifecycleConflictException("Only active dependents can be edited.");
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
    public DependentResult disable(Long id, Long requestingUserId) {
        return transition(id, requestingUserId, requestingUserId, "DISABLE");
    }

    @Transactional
    public DependentResult enable(Long id, Long requestingUserId) {
        return transition(id, requestingUserId, requestingUserId, "ENABLE");
    }

    @Transactional
    public void delete(Long id, Long requestingUserId) { delete(id,requestingUserId,requestingUserId); }

    @Transactional
    public void delete(Long id, Long ownerId, Long actorId) {
        var dependent=findDependentForUpdate(id);
        assertBelongsTo(dependent,ownerId);
        if(deletionGuards.stream().anyMatch(guard -> guard.hasRetainedReferences(id)))
            throw new com.jeepclub.backend.dependents.core.domain.exception.DependentLifecycleConflictException("Dependent has retained Health/Event references.");
        var now=Instant.now(clock);
        dependentRepository.delete(dependent,actorId,now);
        record(actorId,id,"DELETE","DELETE",204,now);
    }

    private Dependent findActiveDependentById(Long id) {
        return dependentRepository.findActiveById(id)
                .orElseThrow(
                        () -> new DependentNotFoundException(id)
                );
    }

    private Dependent findDependentById(Long id) {
        return dependentRepository.findById(id)
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
                || dependentRepository.existsByCpf(cpf)) {

            throw new DependentCpfAlreadyInUseException();
        }
    }

    private void assertCpfAvailableForUpdate(
            String cpf,
            Long dependentId
    ) {
        if (dependentUserPort.existsByCpf(cpf)
                || dependentRepository.existsByCpfAndIdNot(
                cpf,
                dependentId
        )) {

            throw new DependentCpfAlreadyInUseException();
        }
    }

    private DependentResult toResult(Dependent dependent) {
        return DependentResult.from(dependent);
    }

    private String normalizeCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            throw new IllegalArgumentException(
                    "cpf is required"
            );
        }

        return cpf.replaceAll("\\D", "");
    }

    @Transactional(readOnly=true)
    public List<DependentResult> findAllByUserId(Long ownerId, com.jeepclub.backend.dependents.core.domain.enums.DependentStatus status) {
        return dependentRepository.findAllByUserIdAndStatus(ownerId,status).stream().map(this::toResult).toList();
    }

    @Transactional
    public DependentResult archive(Long id,Long ownerId,Long actorId) { return transition(id,ownerId,actorId,"ARCHIVE"); }
    @Transactional
    public DependentResult unarchive(Long id,Long ownerId,Long actorId) { return transition(id,ownerId,actorId,"UNARCHIVE"); }
    @Transactional
    public DependentResult disable(Long id,Long ownerId,Long actorId) { return transition(id,ownerId,actorId,"DISABLE"); }
    @Transactional
    public DependentResult enable(Long id,Long ownerId,Long actorId) { return transition(id,ownerId,actorId,"ENABLE"); }

    private DependentResult transition(Long id,Long owner,Long actor,String action) {
        java.util.Objects.requireNonNull(actor,"Lifecycle actor is required");
        var dependent=findDependentForUpdate(id);
        assertBelongsTo(dependent,owner);
        var before=dependent.getStatus();
        var now=Instant.now(clock);
        switch(action) {
            case "ARCHIVE" -> dependent.archive(now);
            case "UNARCHIVE" -> dependent.unarchive(now);
            case "DISABLE" -> dependent.disable(now);
            case "ENABLE" -> dependent.enable(now);
            default -> throw new IllegalArgumentException("Unknown dependent lifecycle operation.");
        }
        if(before!=dependent.getStatus()) {
            dependent=dependentRepository.save(dependent);
            record(actor,id,action+":"+before+":"+dependent.getStatus(),"PATCH",200,now);
        }
        return toResult(dependent);
    }
    private Dependent findDependentForUpdate(Long id) {
        return dependentRepository.findByIdForUpdate(id).orElseThrow(() -> new DependentNotFoundException(id));
    }
    private void record(Long actor,Long id,String action,String method,int status,Instant now) {
        audit.recordInTransaction(new com.jeepclub.backend.platform.logging.SystemLogEvent(actor,"DEPENDENT_"+action,
                method,"/dependents/"+id,status,com.jeepclub.backend.platform.logging.SystemLogOutcome.SUCCESS,0,null,now));
    }
}
