package com.jeepclub.backend.billing.core.application.service.event;

import com.jeepclub.backend.billing.api.module.*;
import com.jeepclub.backend.billing.core.domain.enums.ChargeRecurrenceType;
import com.jeepclub.backend.billing.core.domain.enums.cycle.PaymentAcceptancePolicy;
import com.jeepclub.backend.billing.core.domain.model.*;
import com.jeepclub.backend.billing.core.domain.model.assignment.*;
import com.jeepclub.backend.billing.core.port.BillingEventPort;
import com.jeepclub.backend.billing.core.repository.*;
import com.jeepclub.backend.billing.core.application.service.chargecycle.AdminChargeCycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class EventBillingService implements EventBillingCommand, EventChargeCatalogQuery, EventFinancialQuery {
    private final ChargeDefinitionRepository definitions;
    private final ChargeAssignmentRepository assignments;
    private final ChargeCycleRepository cycles;
    private final MemberChargeRepository charges;
    private final MemberPaymentRepository payments;
    private final EventChargeContextRepository contexts;
    private final BillingEventPort events;
    private final AdminChargeCycleService administration;
    private final Clock clock;

    @Override @Transactional(readOnly = true)
    public Page<Entry> findEligible(Pageable pageable) {
        return definitions.findEventEligible(pageable).map(d -> new Entry(d.getId(), d.getName(),
            d.getDescription(), d.getDefaultAmount(), d.getRequired(), d.getPaymentAcceptancePolicy().name()));
    }

    @Override @Transactional(readOnly=true)
    public Entry getEligible(Long definitionId) {
        var d = definitions.findById(definitionId).orElseThrow(() -> new EventBillingException(EventBillingException.Reason.CHARGE_NOT_FOUND, "Event charge not found."));
        if (!d.isActive() || d.getRecurrenceType() != ChargeRecurrenceType.ONE_TIME) throw new EventBillingException("Event charge is not eligible.");
        return new Entry(d.getId(), d.getName(), d.getDescription(), d.getDefaultAmount(), d.getRequired(), d.getPaymentAcceptancePolicy().name());
    }

    @Override @Transactional
    public Long createOneTimeEventCharge(Long eventId, String name, String description, BigDecimal amount, boolean required) {
        requireEvent(eventId);
        var definition = definitions.save(ChargeDefinition.create(name, description, amount,
            ChargeRecurrenceType.ONE_TIME, required, PaymentAcceptancePolicy.AFTER_DUE_DATE, null, Instant.now(clock)));
        assignment(eventId, definition.getId());
        return definition.getId();
    }

    @Override @Transactional
    public void ensureAssignment(Long eventId, Long definitionId) {
        requireEvent(eventId);
        eligibleLocked(definitionId);
        assignment(eventId, definitionId);
    }

    @Override @Transactional
    public Long ensureEventMemberCharge(Long eventId, Long definitionId, Long userId, LocalDate dueDate, Long actorId) {
        requireEvent(eventId);
        // Lock an existing parent even before the context exists. Serializes first creation and increments.
        var definition = eligibleLocked(definitionId);
        var context = contexts.find(eventId, definitionId).orElseGet(() -> {
            var assignment = assignment(eventId, definitionId);
            var cycle = cycles.save(ChargeCycle.generate(definition, "EVENT-" + UUID.randomUUID(), dueDate, actorId, Instant.now(clock)));
            var created = new EventChargeContext(eventId, definitionId, assignment.getId(), cycle.getId());
            contexts.save(created);
            return created;
        });
        var cycle = cycles.findById(context.cycleId()).orElseThrow();
        if (!cycle.isGenerated()) throw new EventBillingException("Event charge cycle is closed.");
        return charges.findByChargeCycleIdAndUserId(cycle.getId(), userId).orElseGet(() -> charges.save(
            MemberCharge.create(userId, definitionId, cycle.getId(), cycle.getChargeDefinitionDefaultAmountSnapshot(),
                cycle.getDueDate(), cycle.getChargeDefinitionPaymentAcceptancePolicySnapshot(),
                cycle.getChargeDefinitionLatePaymentGraceDaysSnapshot(), Instant.now(clock)))).getId();
    }

    @Override @Transactional
    public void cancelEventCycles(Long eventId, Long actorId) {
        for (var context : contexts.findByEvent(eventId)) {
            definitions.findByIdForUpdate(context.chargeDefinitionId()).orElseThrow();
            var cycle = cycles.findById(context.cycleId()).orElseThrow();
            if (cycle.isGenerated()) administration.cancel(cycle.getId(), actorId);
        }
    }

    @Override @Transactional(readOnly = true)
    public List<State> findByEvent(Long eventId) {
        var states = new ArrayList<State>();
        var eventContexts = contexts.findByEvent(eventId);
        var eventCharges = charges.findByChargeCycleIdIn(eventContexts.stream().map(EventChargeContext::cycleId).toList());
        var eventPayments = payments.findByMemberChargeIdIn(eventCharges.stream().map(MemberCharge::getId).toList());
        var cycleMetadata=cycles.findByIds(eventContexts.stream().map(EventChargeContext::cycleId).toList()).stream()
            .collect(java.util.stream.Collectors.toMap(ChargeCycle::getId,c->c));
        for (var context : eventContexts) {
            for (var charge : eventCharges.stream().filter(c -> c.getChargeCycleId().equals(context.cycleId())).toList()) {
                var payment = eventPayments.stream().filter(p -> p.getMemberChargeId().equals(charge.getId()))
                    .max(Comparator.comparing(MemberPayment::getCreatedAt).thenComparing(MemberPayment::getId)).orElse(null);
                states.add(new State(context.chargeDefinitionId(), charge.getUserId(), charge.getId(),
                    charge.effectiveStatusAt(LocalDate.now(clock)).name(), payment == null ? null : payment.getStatus().name(),
                    payment == null ? null : payment.getSubmittedAt(),
                    cycleMetadata.containsKey(context.cycleId())?cycleMetadata.get(context.cycleId()).getChargeDefinitionNameSnapshot():null,
                    charge.getFinalAmount(),charge.getChargeCycleId(),charge.getDueDate()));
            }
        }
        return List.copyOf(states);
    }

    private ChargeDefinition eligibleLocked(Long id) {
        var d = definitions.findByIdForUpdate(id).orElseThrow(() -> new EventBillingException(EventBillingException.Reason.CHARGE_NOT_FOUND, "Event charge not found."));
        if (!d.isActive() || d.getRecurrenceType() != ChargeRecurrenceType.ONE_TIME)
            throw new EventBillingException("Event requires an active one-time charge.");
        return d;
    }
    private ChargeAssignment assignment(Long eventId, Long definitionId) {
        var existing = assignments.findByChargeDefinitionId(definitionId, Pageable.unpaged()).stream()
            .filter(a -> a instanceof EventParticipantsChargeAssignment e && e.getEventId().equals(eventId)).findFirst();
        if (existing.isPresent()) {
            if (!existing.get().isActive()) throw new EventBillingException("Event assignment is inactive.");
            return existing.get();
        }
        return assignments.save(EventParticipantsChargeAssignment.create(definitionId, eventId, Instant.now(clock)));
    }
    private void requireEvent(Long id) {
        if (!events.existsEventById(id)) throw new EventBillingException("Event not found.");
    }
}
