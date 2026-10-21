package com.jeepclub.backend.billing.infra.persistence.adapter;

import com.jeepclub.backend.billing.core.domain.model.ChargeDefinition;
import com.jeepclub.backend.billing.core.repository.ChargeDefinitionRepository;
import com.jeepclub.backend.billing.infra.persistence.entity.ChargeDefinitionEntity;
import com.jeepclub.backend.billing.infra.persistence.jpa.ChargeDefinitionJpaRepository;
import com.jeepclub.backend.billing.infra.persistence.mapper.ChargeDefinitionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ChargeDefinitionAdapter implements ChargeDefinitionRepository {
    private final jakarta.persistence.EntityManager entityManager;


    private final ChargeDefinitionJpaRepository jpa;
    private final ChargeDefinitionMapper mapper;
    private final com.jeepclub.backend.billing.infra.persistence.jpa.ChargeCycleJpaRepository cycles;
    private final com.jeepclub.backend.billing.infra.persistence.jpa.MemberChargeJpaRepository charges;
    private final com.jeepclub.backend.billing.infra.persistence.jpa.EventChargeContextJpaRepository eventContexts;
    private final com.jeepclub.backend.billing.infra.persistence.jpa.assignment.ChargeAssignmentJpaRepository assignments;
    private final com.jeepclub.backend.billing.infra.persistence.jpa.ChargeDefinitionDeletionHistoryJpaRepository deletionHistory;

    @Override
    public Optional<ChargeDefinition> findByIdForUpdate(Long id) {
        var locked = jpa.findByIdForUpdate(id);
        // Retain the DB lock while discarding any pre-lock ORM snapshot (MySQL REPEATABLE READ).
        locked.ifPresent(entityManager::detach);
        return locked.flatMap(ignored -> jpa.findByIdForUpdate(id)).map(mapper::toDomain);
    }

    @Override
    public Page<ChargeDefinition> findEventEligible(Pageable pageable) {
        return jpa.findByStatusAndRecurrenceType(
            com.jeepclub.backend.billing.core.domain.enums.definition.ChargeDefinitionStatus.ACTIVE,
            com.jeepclub.backend.billing.core.domain.enums.ChargeRecurrenceType.ONE_TIME, pageable).map(mapper::toDomain);
    }

    @Override
    public ChargeDefinition save(ChargeDefinition chargeDefinition) {
        ChargeDefinitionEntity entity = mapper.toEntity(chargeDefinition);
        ChargeDefinitionEntity savedEntity = jpa.save(entity);

        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<ChargeDefinition> findById(Long id) {
        return jpa.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Page<ChargeDefinition> findAll(Pageable pageable) {
        return jpa.findAll(pageable)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByName(String name) {
        return jpa.existsByName(name);
    }

    @Override
    public boolean existsByNameAndIdNot(String name, Long id) {
        return jpa.existsByNameAndIdNot(name, id);
    }

    @Override
    public void deleteWithHistory(Long definitionId, Long actorId, java.time.Instant now) {
        java.util.Objects.requireNonNull(actorId, "Deletion actor is required");
        var definition = findByIdForUpdate(definitionId).orElseThrow(() ->
                new com.jeepclub.backend.billing.core.application.exception.definition.ChargeDefinitionNotFoundException("Charge definition not found."));
        if (cycles.findFirstByChargeDefinitionIdOrderById(definitionId).isPresent()
                || charges.findFirstByChargeDefinitionIdOrderById(definitionId).isPresent()
                || assignments.findFirstByChargeDefinitionIdOrderById(definitionId).isPresent()
                || eventContexts.findFirstByChargeDefinitionIdOrderById(definitionId).isPresent()) {
            throw new com.jeepclub.backend.billing.core.domain.exception.definition.ChargeDefinitionDeletionConflictException();
        }
        deletionHistory.saveAndFlush(new com.jeepclub.backend.billing.infra.persistence.entity.ChargeDefinitionDeletionHistoryEntity(
                definition.getId(), definition.getName(), definition.getDescription(), definition.getDefaultAmount(),
                definition.getRecurrenceType().name(), definition.getRequired(), definition.getPaymentAcceptancePolicy().name(),
                definition.getLatePaymentGraceDays(), definition.getStatus().name(), definition.getCreatedAt(),
                definition.getUpdatedAt(), definition.getArchivedAt(), actorId, now, "ADMINISTRATIVE_DELETE", 1));
        jpa.deleteById(definitionId);
        jpa.flush();
    }
}
