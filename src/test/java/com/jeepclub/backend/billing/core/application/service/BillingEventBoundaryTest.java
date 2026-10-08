package com.jeepclub.backend.billing.core.application.service;

import com.jeepclub.backend.billing.core.application.service.event.EventBillingService;
import com.jeepclub.backend.billing.core.application.service.chargecycle.AdminChargeCycleService;
import com.jeepclub.backend.billing.core.application.query.*;
import com.jeepclub.backend.billing.api.module.*;
import com.jeepclub.backend.billing.core.domain.model.*;
import com.jeepclub.backend.billing.core.domain.model.assignment.*;
import com.jeepclub.backend.billing.core.repository.*;
import com.jeepclub.backend.billing.core.port.BillingEventPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import java.util.*;
import static com.jeepclub.backend.billing.support.BillingFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class BillingEventBoundaryTest {
    @Mock ChargeDefinitionRepository definitions;
    @Mock ChargeAssignmentRepository assignments;
    @Mock ChargeCycleRepository cycles;
    @Mock MemberChargeRepository charges;
    @Mock MemberPaymentRepository payments;
    @Mock EventChargeContextRepository contexts;
    @Mock BillingEventPort events;
    @Mock AdminChargeCycleService administration;
    EventBillingService service() { return new EventBillingService(definitions, assignments, cycles, charges, payments, contexts, events, administration, CLOCK); }
    @Test void catalogRejectsAbsentInactiveAndRecurringDefinitions() {
        assertThatThrownBy(() -> service().getEligible(404L)).isInstanceOf(EventBillingException.class);
        var d = definition(); when(definitions.findById(3L)).thenReturn(Optional.of(d));
        assertThat(service().getEligible(3L).name()).isEqualTo("monthly fee");
        d.deactivate(NOW); assertThatThrownBy(() -> service().getEligible(3L)).isInstanceOf(EventBillingException.class);
        d.activate(NOW); d.update("fee", null, AMOUNT, com.jeepclub.backend.billing.core.domain.enums.ChargeRecurrenceType.MONTHLY, true,
                com.jeepclub.backend.billing.core.domain.enums.cycle.PaymentAcceptancePolicy.AFTER_DUE_DATE, null, NOW);
        assertThatThrownBy(() -> service().getEligible(3L)).isInstanceOf(EventBillingException.class);
        when(definitions.findEventEligible(Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(definition())));
        assertThat(service().findEligible(Pageable.unpaged()).getContent()).singleElement()
                .satisfies(entry -> { assertThat(entry.id()).isEqualTo(3L); assertThat(entry.name()).isEqualTo("monthly fee"); });
    }
    @Test void inlineDefinitionForcesOneTimeUnlimitedPaymentAndEventAssignment() {
        when(events.existsEventById(30L)).thenReturn(true);
        when(definitions.save(any())).thenAnswer(i -> { var d = (ChargeDefinition)i.getArgument(0);
            assertThat(d.getRecurrenceType()).isEqualTo(com.jeepclub.backend.billing.core.domain.enums.ChargeRecurrenceType.ONE_TIME);
            assertThat(d.getPaymentAcceptancePolicy()).isEqualTo(com.jeepclub.backend.billing.core.domain.enums.cycle.PaymentAcceptancePolicy.AFTER_DUE_DATE);
            return definition(); });
        when(assignments.findByChargeDefinitionId(3L, Pageable.unpaged())).thenReturn(Page.empty());
        assertThat(service().createOneTimeEventCharge(30L, "fee", null, AMOUNT, false)).isEqualTo(3L);
        verify(assignments).save(argThat(a -> a instanceof EventParticipantsChargeAssignment e && e.getEventId().equals(30L)));
    }
    @Test void existingDebtIsReturnedWithoutNewWritesUnderDefinitionLock() {
        when(events.existsEventById(30L)).thenReturn(true); when(definitions.findByIdForUpdate(3L)).thenReturn(Optional.of(definition()));
        when(contexts.find(30L, 3L)).thenReturn(Optional.of(new EventChargeContext(30L, 3L, 5L, 4L)));
        when(cycles.findById(4L)).thenReturn(Optional.of(cycle())); when(charges.findByChargeCycleIdAndUserId(4L, 10L)).thenReturn(Optional.of(charge()));
        assertThat(service().ensureEventMemberCharge(30L, 3L, 10L, DUE.plusDays(5), 99L)).isEqualTo(2L);
        verify(charges, never()).save(any()); verify(cycles, never()).save(any()); verify(contexts, never()).save(any());
    }
    @Test void cancellationDelegatesOnlyGeneratedCyclesBelongingToEvent() {
        var generated = cycle(); var finished = withId(cycle(), 8L); finished.finish(99L, NOW);
        when(contexts.findByEvent(30L)).thenReturn(List.of(new EventChargeContext(30L, 3L, 5L, 4L), new EventChargeContext(30L, 6L, 7L, 8L)));
        when(definitions.findByIdForUpdate(anyLong())).thenReturn(Optional.of(definition()));
        when(cycles.findById(4L)).thenReturn(Optional.of(generated)); when(cycles.findById(8L)).thenReturn(Optional.of(finished));
        service().cancelEventCycles(30L, 99L); verify(administration).cancel(4L, 99L); verify(administration, never()).cancel(8L, 99L);
        verifyNoInteractions(charges, payments); // BACK-464: characterized current behavior, policy change is separate.
    }
    @Test void inactiveExistingEventAssignmentIsRejectedAfterParentLock() {
        var a = EventParticipantsChargeAssignment.create(3L, 30L, NOW); a.deactivate(NOW);
        when(events.existsEventById(30L)).thenReturn(true); when(definitions.findByIdForUpdate(3L)).thenReturn(Optional.of(definition()));
        when(assignments.findByChargeDefinitionId(3L, Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(a)));
        assertThatThrownBy(() -> service().ensureAssignment(30L, 3L)).isInstanceOf(EventBillingException.class);
        verify(assignments, never()).save(any());
    }
    @Test void existingClosedContextCannotReceiveAnotherDebtAndMissingEventHasControlledFailure() {
        assertThatThrownBy(() -> service().ensureAssignment(404L, 3L)).isInstanceOf(EventBillingException.class);
        when(events.existsEventById(30L)).thenReturn(true); when(definitions.findByIdForUpdate(3L)).thenReturn(Optional.of(definition()));
        when(contexts.find(30L, 3L)).thenReturn(Optional.of(new EventChargeContext(30L, 3L, 5L, 4L)));
        var c = cycle(); c.finish(99L, NOW); when(cycles.findById(4L)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service().ensureEventMemberCharge(30L, 3L, 10L, DUE, 99L)).isInstanceOf(EventBillingException.class);
        verifyNoInteractions(charges);
    }
    @Test void newEventDebtPersistsContextAndReusesTheMatchingEventAssignment() {
        when(events.existsEventById(30L)).thenReturn(true);
        when(definitions.findByIdForUpdate(3L)).thenReturn(Optional.of(definition()));
        var wrongEvent = EventParticipantsChargeAssignment.reconstitute(6L, 3L, 31L, true, NOW, null);
        var rightEvent = EventParticipantsChargeAssignment.reconstitute(5L, 3L, 30L, true, NOW, null);
        when(assignments.findByChargeDefinitionId(3L, Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(wrongEvent, rightEvent)));
        when(cycles.save(any())).thenAnswer(i -> withId(i.getArgument(0), 4L));
        when(cycles.findById(4L)).thenReturn(Optional.of(cycle()));
        when(charges.save(any())).thenReturn(charge());
        assertThat(service().ensureEventMemberCharge(30L, 3L, 10L, DUE, 99L)).isEqualTo(2L);
        verify(contexts).save(new EventChargeContext(30L, 3L, 5L, 4L));
        verify(assignments, never()).save(any());
        verify(charges).save(argThat(c -> c.getUserId().equals(10L) && c.getChargeCycleId().equals(4L)
                && c.getFinalAmount().compareTo(AMOUNT) == 0));
    }
    @Test void newEventContextCreatesItsAssignmentWhenNoneExists() {
        when(events.existsEventById(30L)).thenReturn(true);
        when(definitions.findByIdForUpdate(3L)).thenReturn(Optional.of(definition()));
        when(assignments.findByChargeDefinitionId(3L, Pageable.unpaged())).thenReturn(Page.empty());
        when(assignments.save(any())).thenReturn(EventParticipantsChargeAssignment.reconstitute(5L, 3L, 30L, true, NOW, null));
        when(cycles.save(any())).thenAnswer(i -> withId(i.getArgument(0), 4L));
        when(cycles.findById(4L)).thenReturn(Optional.of(cycle()));
        when(charges.save(any())).thenReturn(charge());
        assertThat(service().ensureEventMemberCharge(30L, 3L, 10L, DUE, 99L)).isEqualTo(2L);
        verify(contexts).save(new EventChargeContext(30L, 3L, 5L, 4L));
    }
    @Test void missingLockedDefinitionReturnsControlledEventFailure() {
        when(events.existsEventById(30L)).thenReturn(true);
        assertThatThrownBy(() -> service().ensureAssignment(30L, 404L)).isInstanceOf(EventBillingException.class);
        verifyNoInteractions(assignments, cycles, charges);
    }
    @Test void financialQuerySeparatesCyclesSelectsLatestPaymentAndKeepsUnpaidRows() {
        var otherCharge = MemberCharge.reconstitute(12L, 20L, 6L, 8L, AMOUNT, AMOUNT, DUE,
                com.jeepclub.backend.billing.core.domain.enums.cycle.PaymentAcceptancePolicy.UNTIL_DUE_DATE,
                null, DUE, com.jeepclub.backend.billing.core.domain.enums.charge.MemberChargeStatus.PENDING, NOW, null, null, null);
        var mapper = new com.jeepclub.backend.billing.infra.persistence.mapper.MemberPaymentMapper();
        var older = payment();
        var latestEntity = mapper.toEntity(payment());
        org.springframework.test.util.ReflectionTestUtils.setField(latestEntity, "id", 9L);
        org.springframework.test.util.ReflectionTestUtils.setField(latestEntity, "createdAt", NOW.plusSeconds(1));
        org.springframework.test.util.ReflectionTestUtils.setField(latestEntity, "submittedAt", NOW.plusSeconds(2));
        var latest = mapper.toDomain(latestEntity); latest.reject(99L, "reason", NOW.plusSeconds(3));
        when(contexts.findByEvent(30L)).thenReturn(List.of(new EventChargeContext(30L, 3L, 5L, 4L), new EventChargeContext(30L, 6L, 7L, 8L)));
        when(charges.findByChargeCycleIdIn(List.of(4L, 8L))).thenReturn(List.of(charge(), otherCharge));
        when(payments.findByMemberChargeIdIn(List.of(2L, 12L))).thenReturn(List.of(older, latest));
        when(cycles.findByIds(List.of(4L, 8L))).thenReturn(List.of(cycle()));
        var states = service().findByEvent(30L);
        assertThat(states).hasSize(2);
        assertThat(states.get(0)).isEqualTo(new EventFinancialQuery.State(3L, 10L, 2L, "PENDING", "REJECTED", NOW.plusSeconds(2), "monthly fee", AMOUNT, 4L, DUE));
        assertThat(states.get(1)).isEqualTo(new EventFinancialQuery.State(6L, 20L, 12L, "PENDING", null, null, null, AMOUNT, 8L, DUE));
    }
    @Test void chargeDefinitionQueryDoesNotExposeFinancialInternalsAndInvalidReferencesAreFalse() {
        var query = new ChargeDefinitionQueryService(definitions);
        assertThat(query.isActive(null)).isFalse(); assertThat(query.isActive(0L)).isFalse(); assertThat(query.isActive(-1L)).isFalse();
        assertThat(query.isActive(404L)).isFalse(); var d = definition(); when(definitions.findById(3L)).thenReturn(Optional.of(d));
        assertThat(query.isActive(3L)).isTrue(); d.deactivate(NOW); assertThat(query.isActive(3L)).isFalse();
    }
    @Test void financeReportBatchLimitAccepts500AndRejects501BeforeDatabaseAccess() {
        var repository = mock(EventFinanceReportRepository.class); var query = new EventFinanceReportQueryService(repository);
        var ids = Collections.nCopies(500, 3L); query.definitions(ids); verify(repository).definitions(ids);
        assertThatThrownBy(() -> query.definitions(Collections.nCopies(501, 3L))).isInstanceOf(IllegalArgumentException.class);
        query.paymentCounts(30L); verify(repository).paymentCounts(30L); query.requireWithinLimit(30L); verify(repository).requireWithinLimit(30L);
    }
}
