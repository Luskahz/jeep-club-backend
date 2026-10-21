package com.jeepclub.backend.billing.core.application.service.chargedefinition;

import com.jeepclub.backend.billing.core.application.exception.definition.ChargeDefinitionAlreadyExistsException;
import com.jeepclub.backend.billing.core.application.exception.definition.ChargeDefinitionNotFoundException;
import com.jeepclub.backend.billing.core.application.result.ChargeDefinitionResult;
import com.jeepclub.backend.billing.core.domain.enums.ChargeRecurrenceType;
import com.jeepclub.backend.billing.core.domain.enums.cycle.PaymentAcceptancePolicy;
import com.jeepclub.backend.billing.core.domain.model.ChargeDefinition;
import com.jeepclub.backend.billing.core.domain.model.assignment.ChargeAssignment;
import com.jeepclub.backend.billing.core.repository.ChargeAssignmentRepository;
import com.jeepclub.backend.billing.core.repository.ChargeDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminChargeDefinitionService {

    private final ChargeDefinitionRepository chargeDefinitionRepository;
    private final ChargeAssignmentRepository chargeAssignmentRepository;
    private final Clock clock;
    private final java.util.List<com.jeepclub.backend.billing.api.module.spi.ChargeDefinitionDeletionGuard> deletionGuards;
    private final com.jeepclub.backend.platform.logging.SystemLogService audit;

    @Transactional
    public ChargeDefinitionResult create(
            String name,
            String description,
            BigDecimal defaultAmount,
            ChargeRecurrenceType recurrenceType,
            Boolean required,
            PaymentAcceptancePolicy paymentAcceptancePolicy,
            Integer latePaymentGraceDays
    ) {
        Objects.requireNonNull(recurrenceType, "recurrenceType cannot be null");
        Objects.requireNonNull(paymentAcceptancePolicy, "paymentAcceptancePolicy cannot be null");

        String normalizedName = normalizeName(name);

        if (chargeDefinitionRepository.existsByName(normalizedName)) {
            throw new ChargeDefinitionAlreadyExistsException(
                    "Charge definition name already exists."
            );
        }

        Instant now = Instant.now(clock);

        ChargeDefinition chargeDefinition = ChargeDefinition.create(
                normalizedName,
                description,
                defaultAmount,
                recurrenceType,
                required,
                paymentAcceptancePolicy,
                latePaymentGraceDays,
                now
        );

        ChargeDefinition savedChargeDefinition = chargeDefinitionRepository.save(chargeDefinition);

        return ChargeDefinitionResult.from(savedChargeDefinition);
    }

    @Transactional(readOnly = true)
    public Page<ChargeDefinitionResult> findAll(Pageable pageable) {
        Objects.requireNonNull(pageable, "pageable cannot be null");

        return chargeDefinitionRepository.findAll(pageable)
                .map(ChargeDefinitionResult::from);
    }

    @Transactional(readOnly = true)
    public ChargeDefinitionResult findById(Long id) {
        ChargeDefinition chargeDefinition = findChargeDefinitionOrThrow(id);

        return ChargeDefinitionResult.from(chargeDefinition);
    }

    @Transactional
    public ChargeDefinitionResult activate(Long id) {
        ChargeDefinition chargeDefinition = findChargeDefinitionForUpdate(id);

        chargeDefinition.activate(Instant.now(clock));

        ChargeDefinition savedChargeDefinition = chargeDefinitionRepository.save(chargeDefinition);

        return ChargeDefinitionResult.from(savedChargeDefinition);
    }

    @Transactional
    public ChargeDefinitionResult deactivate(Long id) {
        ChargeDefinition chargeDefinition = findChargeDefinitionForUpdate(id);

        chargeDefinition.deactivate(Instant.now(clock));

        ChargeDefinition savedChargeDefinition = chargeDefinitionRepository.save(chargeDefinition);

        return ChargeDefinitionResult.from(savedChargeDefinition);
    }

    @Transactional
    public ChargeDefinitionResult archive(Long id) {
        ChargeDefinition chargeDefinition = findChargeDefinitionForUpdate(id);

        Instant now = Instant.now(clock);

        chargeDefinition.archive(now);

        deactivateActiveAssignments(
                chargeDefinition.getId(),
                now
        );

        ChargeDefinition savedChargeDefinition = chargeDefinitionRepository.save(chargeDefinition);

        return ChargeDefinitionResult.from(savedChargeDefinition);
    }

    @Transactional
    public ChargeDefinitionResult update(
            Long id,
            String name,
            String description,
            BigDecimal defaultAmount,
            ChargeRecurrenceType recurrenceType,
            Boolean required,
            PaymentAcceptancePolicy paymentAcceptancePolicy,
            Integer latePaymentGraceDays
    ) {
        Objects.requireNonNull(recurrenceType, "recurrenceType cannot be null");
        Objects.requireNonNull(paymentAcceptancePolicy, "paymentAcceptancePolicy cannot be null");

        ChargeDefinition chargeDefinition = findChargeDefinitionForUpdate(id);

        String normalizedName = normalizeName(name);

        if (chargeDefinitionRepository.existsByNameAndIdNot(normalizedName, id)) {
            throw new ChargeDefinitionAlreadyExistsException(
                    "Charge definition name already exists."
            );
        }

        chargeDefinition.update(
                normalizedName,
                description,
                defaultAmount,
                recurrenceType,
                required,
                paymentAcceptancePolicy,
                latePaymentGraceDays,
                Instant.now(clock)
        );

        ChargeDefinition savedChargeDefinition = chargeDefinitionRepository.save(chargeDefinition);

        return ChargeDefinitionResult.from(savedChargeDefinition);
    }

    private void deactivateActiveAssignments(
            Long chargeDefinitionId,
            Instant now
    ) {
        chargeAssignmentRepository.findByChargeDefinitionId(
                        chargeDefinitionId,
                        Pageable.unpaged()
                )
                .stream()
                .filter(ChargeAssignment::isActive)
                .forEach(chargeAssignment -> {
                    chargeAssignment.deactivate(now);
                    chargeAssignmentRepository.save(chargeAssignment);
                });
    }

    private ChargeDefinition findChargeDefinitionOrThrow(Long id) {
        Objects.requireNonNull(id, "id cannot be null");

        return chargeDefinitionRepository.findById(id)
                .orElseThrow(() -> new ChargeDefinitionNotFoundException(
                        "Charge definition not found."
                ));
    }

    private static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Charge definition name cannot be blank.");
        }

        return name.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public void delete(Long definitionId, Long actorId) {
        java.util.Objects.requireNonNull(actorId, "Deletion actor is required");
        findChargeDefinitionForUpdate(definitionId);
        if (deletionGuards.stream().anyMatch(guard -> guard.hasRetainedReferences(definitionId))) {
            throw new com.jeepclub.backend.billing.core.domain.exception.definition.ChargeDefinitionDeletionConflictException();
        }
        Instant now = Instant.now(clock);
        chargeDefinitionRepository.deleteWithHistory(definitionId, actorId, now);
        audit.recordInTransaction(new com.jeepclub.backend.platform.logging.SystemLogEvent(actorId,
                "BILLING_CHARGE_DEFINITION_DELETE", "DELETE", "/billing/charge-definitions/" + definitionId,
                204, com.jeepclub.backend.platform.logging.SystemLogOutcome.SUCCESS, 0, null, now));
    }

    private ChargeDefinition findChargeDefinitionForUpdate(Long id) {
        return chargeDefinitionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ChargeDefinitionNotFoundException("Charge definition not found."));
    }
    @Transactional
    public ChargeDefinitionResult activate(Long id,Long actorId) {
        var definition=findChargeDefinitionForUpdate(id);
        if(definition.getStatus().name().equals("ACTIVE")) return ChargeDefinitionResult.from(definition);
        var result=activate(id);
        audit.recordInTransaction(new com.jeepclub.backend.platform.logging.SystemLogEvent(actorId,"BILLING_CHARGE_DEFINITION_ACTIVATE",
                "PATCH","/billing/charge-definitions/"+id+"/activate",200,com.jeepclub.backend.platform.logging.SystemLogOutcome.SUCCESS,0,null,Instant.now(clock)));
        return result;
    }
    @Transactional
    public ChargeDefinitionResult deactivate(Long id,Long actorId) {
        var definition=findChargeDefinitionForUpdate(id);
        if(definition.getStatus().name().equals("INACTIVE")) return ChargeDefinitionResult.from(definition);
        var result=deactivate(id);
        audit.recordInTransaction(new com.jeepclub.backend.platform.logging.SystemLogEvent(actorId,"BILLING_CHARGE_DEFINITION_DEACTIVATE",
                "PATCH","/billing/charge-definitions/"+id+"/deactivate",200,com.jeepclub.backend.platform.logging.SystemLogOutcome.SUCCESS,0,null,Instant.now(clock)));
        return result;
    }
    @Transactional
    public ChargeDefinitionResult archive(Long id,Long actorId) {
        var definition=findChargeDefinitionForUpdate(id);
        if(definition.getStatus().name().equals("ARCHIVED")) return ChargeDefinitionResult.from(definition);
        var result=archive(id);
        audit.recordInTransaction(new com.jeepclub.backend.platform.logging.SystemLogEvent(actorId,"BILLING_CHARGE_DEFINITION_ARCHIVE",
                "PATCH","/billing/charge-definitions/"+id+"/archive",200,com.jeepclub.backend.platform.logging.SystemLogOutcome.SUCCESS,0,null,Instant.now(clock)));
        return result;
    }

}
