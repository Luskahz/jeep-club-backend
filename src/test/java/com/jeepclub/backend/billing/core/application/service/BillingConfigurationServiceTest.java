package com.jeepclub.backend.billing.core.application.service;

import com.jeepclub.backend.billing.core.application.service.chargedefinition.AdminChargeDefinitionService;
import com.jeepclub.backend.billing.core.application.service.chargeassignment.AdminChargeAssignmentService;
import com.jeepclub.backend.billing.core.application.service.chargecycle.AdminChargeCycleService;
import com.jeepclub.backend.billing.core.application.service.memberrefund.AdminMemberRefundService;
import com.jeepclub.backend.billing.core.application.exception.assignment.*;
import com.jeepclub.backend.billing.core.application.exception.definition.*;
import com.jeepclub.backend.billing.core.application.exception.cycle.*;
import com.jeepclub.backend.billing.core.domain.model.*;
import com.jeepclub.backend.billing.core.domain.model.assignment.*;
import com.jeepclub.backend.billing.core.domain.enums.*;
import com.jeepclub.backend.billing.core.domain.enums.cycle.*;
import com.jeepclub.backend.billing.core.repository.*;
import com.jeepclub.backend.billing.core.port.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import java.math.BigDecimal;
import java.util.*;
import static com.jeepclub.backend.billing.support.BillingFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class BillingConfigurationServiceTest {
    @Mock ChargeDefinitionRepository definitions;
    @Mock ChargeAssignmentRepository assignments;
    @Mock ChargeCycleRepository cycles;
    @Mock MemberChargeRepository charges;
    @Mock MemberPaymentRepository payments;
    @Mock AdminMemberRefundService refunds;
    @Mock BillingMembershipPort members;
    @Mock BillingAuthorizationPort roles;
    @Mock BillingEventPort events;
    AdminChargeDefinitionService definitionService;
    AdminChargeAssignmentService assignmentService;
    AdminChargeCycleService cycleService;
    @BeforeEach void setup() {
        definitionService = new AdminChargeDefinitionService(definitions, assignments, CLOCK);
        assignmentService = new AdminChargeAssignmentService(assignments, definitions, events, members, roles, CLOCK);
        cycleService = new AdminChargeCycleService(cycles, definitions, assignments, charges, refunds, members, roles, events, CLOCK, payments);
    }
    @Test void createCanonicalizesNameAndRejectsDuplicateBeforeSaving() {
        when(definitions.save(any())).thenAnswer(i -> i.getArgument(0));
        var result = definitionService.create("  MeNsAl  ", " desc ", AMOUNT, ChargeRecurrenceType.MONTHLY, true, PaymentAcceptancePolicy.AFTER_DUE_DATE, null);
        assertThat(result.name()).isEqualTo("mensal"); assertThat(result.createdAt()).isEqualTo(NOW);
        verify(definitions).existsByName("mensal");
        when(definitions.existsByName("mensal")).thenReturn(true);
        assertThatThrownBy(() -> definitionService.create("MENSAL", null, AMOUNT, ChargeRecurrenceType.MONTHLY, true,
                PaymentAcceptancePolicy.AFTER_DUE_DATE, null)).isInstanceOf(ChargeDefinitionAlreadyExistsException.class);
        verify(definitions, times(1)).save(any());
    }
    @Test void archiveDeactivatesOnlyActiveAssignmentsAndPreservesHistory() {
        var d = definition(); var active = UserChargeAssignment.create(3L, 10L, NOW); var inactive = AllMembersChargeAssignment.create(3L, NOW); inactive.deactivate(NOW);
        when(definitions.findById(3L)).thenReturn(Optional.of(d));
        when(assignments.findByChargeDefinitionId(3L, Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(active, inactive)));
        when(definitions.save(d)).thenReturn(d);
        definitionService.archive(3L);
        assertThat(d.getArchivedAt()).isEqualTo(NOW); assertThat(active.isActive()).isFalse();
        verify(assignments).save(active); verify(assignments, never()).save(inactive);
        verifyNoInteractions(cycles, charges, payments);
    }
    @Test void definitionLifecycleAndUpdateRespectUniqueness() {
        var d = definition(); when(definitions.findById(3L)).thenReturn(Optional.of(d)); when(definitions.save(d)).thenReturn(d);
        definitionService.deactivate(3L); assertThat(d.isActive()).isFalse(); definitionService.activate(3L); assertThat(d.isActive()).isTrue();
        definitionService.update(3L, " FEE ", null, BigDecimal.TEN, ChargeRecurrenceType.YEARLY, false, PaymentAcceptancePolicy.AFTER_DUE_DATE, null);
        assertThat(d.getName()).isEqualTo("fee"); assertThat(d.getDefaultAmount()).isEqualByComparingTo(BigDecimal.TEN);
        when(definitions.existsByNameAndIdNot("fee", 3L)).thenReturn(true);
        assertThatThrownBy(() -> definitionService.update(3L, " FEE ", null, AMOUNT, ChargeRecurrenceType.YEARLY, true,
                PaymentAcceptancePolicy.AFTER_DUE_DATE, null)).isInstanceOf(ChargeDefinitionAlreadyExistsException.class);
        assertThatThrownBy(() -> definitionService.findById(404L)).isInstanceOf(ChargeDefinitionNotFoundException.class);
    }
    @ParameterizedTest @ValueSource(ints = {0, 1, 2, 3})
    void assignmentCreationChecksItsBoundaryAndDuplicate(int audience) {
        when(definitions.findById(3L)).thenReturn(Optional.of(definition()));
        when(assignments.save(any())).thenAnswer(i -> i.getArgument(0));
        switch (audience) {
            case 0 -> { assertThat(assignmentService.assignToAllMembers(3L).active()).isTrue(); when(assignments.existsAllMembersAssignmentByChargeDefinitionId(3L)).thenReturn(true); }
            case 1 -> { when(members.existsActiveMemberByUserId(10L)).thenReturn(true); assertThat(assignmentService.assignToUser(3L, 10L).userId()).isEqualTo(10L); when(assignments.existsUserAssignmentByChargeDefinitionIdAndUserId(3L, 10L)).thenReturn(true); }
            case 2 -> { when(roles.existsActiveRoleById(20L)).thenReturn(true); assertThat(assignmentService.assignToRole(3L, 20L).roleId()).isEqualTo(20L); when(assignments.existsRoleAssignmentByChargeDefinitionIdAndRoleId(3L, 20L)).thenReturn(true); }
            default -> { when(events.existsEventById(30L)).thenReturn(true); assertThat(assignmentService.assignToEventParticipants(3L, 30L).eventId()).isEqualTo(30L); when(assignments.existsEventParticipantsAssignmentByChargeDefinitionIdAndEventId(3L, 30L)).thenReturn(true); }
        }
        assertThatThrownBy(() -> assign(audience)).isInstanceOf(ChargeAssignmentAlreadyExistsException.class);
        verify(assignments, times(1)).save(any());
    }
    private void assign(int audience) {
        switch (audience) { case 0 -> assignmentService.assignToAllMembers(3L); case 1 -> assignmentService.assignToUser(3L, 10L); case 2 -> assignmentService.assignToRole(3L, 20L); default -> assignmentService.assignToEventParticipants(3L, 30L); }
    }
    @ParameterizedTest @ValueSource(ints = {1, 2, 3})
    void missingAssignmentTargetIsRejected(int audience) {
        when(definitions.findById(3L)).thenReturn(Optional.of(definition()));
        assertThatThrownBy(() -> assign(audience)).isInstanceOf(BillingAssignmentTargetNotFoundException.class);
        verify(assignments, never()).save(any());
    }
    @Test void inactiveDefinitionCannotReceiveOrActivateAssignmentsButCanDeactivate() {
        var d = definition(); d.deactivate(NOW); var a = AllMembersChargeAssignment.create(3L, NOW);
        when(definitions.findById(3L)).thenReturn(Optional.of(d)); when(assignments.findById(1L)).thenReturn(Optional.of(a));
        assertThatThrownBy(() -> assignmentService.assignToAllMembers(3L)).isInstanceOf(ChargeDefinitionCannotChangeAssignmentsException.class);
        assertThatThrownBy(() -> assignmentService.activate(1L)).isInstanceOf(ChargeDefinitionCannotChangeAssignmentsException.class);
        when(assignments.save(a)).thenReturn(a); assignmentService.deactivate(1L); assertThat(a.isActive()).isFalse();
        d.activate(NOW); assignmentService.activate(1L); assertThat(a.isActive()).isTrue();
        d.archive(NOW); assertThatThrownBy(() -> assignmentService.deactivate(1L)).isInstanceOf(ChargeDefinitionCannotChangeAssignmentsException.class);
        assertThatThrownBy(() -> assignmentService.findById(404L)).isInstanceOf(ChargeAssignmentNotFoundException.class);
    }
    @Test void cycleGenerationDeduplicatesAudiencesFiltersInactiveUsersAndSkipsExistingCharge() {
        when(definitions.findById(3L)).thenReturn(Optional.of(definition()));
        var ignored = UserChargeAssignment.create(3L, 99L, NOW); ignored.deactivate(NOW);
        when(assignments.findByChargeDefinitionId(3L, Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(
                AllMembersChargeAssignment.create(3L, NOW), UserChargeAssignment.create(3L, 10L, NOW), RoleChargeAssignment.create(3L, 20L, NOW),
                EventParticipantsChargeAssignment.create(3L, 30L, NOW), ignored)));
        when(members.findActiveMemberUserIds()).thenReturn(List.of(10L, 11L));
        when(members.existsActiveMemberByUserId(anyLong())).thenAnswer(i -> Set.of(10L, 12L).contains(i.getArgument(0)));
        when(roles.findUserIdsByRoleId(20L)).thenReturn(List.of(10L, 12L, 13L));
        when(events.findConfirmedParticipantUserIdsByEventId(30L)).thenReturn(List.of(11L, 14L));
        when(charges.existsByUserIdAndChargeCycleId(anyLong(), eq(4L))).thenAnswer(i -> i.getArgument(0).equals(11L));
        when(cycles.save(any())).thenAnswer(i -> withId(i.getArgument(0), 4L));
        var result = cycleService.generate(3L, " SEP ", DUE, 99L);
        assertThat(result.createdMemberCharges()).isEqualTo(3);
        var captor = ArgumentCaptor.forClass(MemberCharge.class); verify(charges, times(3)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(MemberCharge::getUserId).containsExactly(10L, 12L, 14L);
        assertThat(captor.getAllValues()).allSatisfy(c -> { assertThat(c.getChargeCycleId()).isEqualTo(4L); assertThat(c.getFinalAmount()).isEqualByComparingTo(AMOUNT); assertThat(c.getDueDate()).isEqualTo(DUE); });
        verify(members, never()).existsActiveMemberByUserId(99L); verify(cycles).existsByChargeDefinitionIdAndCode(3L, "SEP");
    }
    @Test void generationRejectsInactiveDuplicateAndEmptyAudience() {
        var d = definition(); when(definitions.findById(3L)).thenReturn(Optional.of(d)); d.deactivate(NOW);
        assertThatThrownBy(() -> cycleService.generate(3L, "code", DUE, 99L)).isInstanceOf(InactiveChargeDefinitionException.class);
        d.activate(NOW); when(cycles.existsByChargeDefinitionIdAndCode(3L, "code")).thenReturn(true);
        assertThatThrownBy(() -> cycleService.generate(3L, "code", DUE, 99L)).isInstanceOf(ChargeCycleAlreadyExistsException.class);
        when(cycles.existsByChargeDefinitionIdAndCode(3L, "code")).thenReturn(false);
        when(assignments.findByChargeDefinitionId(3L, Pageable.unpaged())).thenReturn(Page.empty());
        assertThatThrownBy(() -> cycleService.generate(3L, "code", DUE, 99L)).isInstanceOf(ChargeCycleWithoutAssignmentsException.class);
        verify(cycles, never()).save(any()); verify(charges, never()).save(any());
    }
    @Test void cancellationLocksPaymentsInIdOrderThenReloadsChargesAndCreatesRefunds() {
        var c = cycle(); var open = charge(); var becamePaid = charge(); becamePaid.markAsPaid(NOW, NOW);
        when(cycles.findById(4L)).thenReturn(Optional.of(c)); when(cycles.save(c)).thenReturn(c);
        when(charges.findByChargeCycleId(4L)).thenReturn(List.of(open)); when(payments.findByMemberChargeIdIn(List.of(2L))).thenReturn(List.of(payment()));
        when(charges.findOpenByChargeCycleId(4L)).thenReturn(List.of(open)); when(charges.findByIdForUpdate(2L)).thenReturn(Optional.of(becamePaid));
        cycleService.cancel(4L, 99L);
        var order = inOrder(payments, charges, cycles, refunds);
        order.verify(payments).findByIdForUpdate(1L); order.verify(charges).findByIdForUpdate(2L);
        order.verify(cycles).save(c); order.verify(refunds).createEligibilityForCanceledCycle(4L, 99L, NOW);
        assertThat(becamePaid.isPaid()).isTrue(); verify(charges, never()).save(any());
    }
    @Test void cancellationCancelsStillOpenChargeAndFinishArchiveHaveNoFinancialSideEffects() {
        var c = cycle(); var debt = charge(); when(cycles.findById(4L)).thenReturn(Optional.of(c)); when(cycles.save(c)).thenReturn(c);
        when(charges.findOpenByChargeCycleId(4L)).thenReturn(List.of(debt)); when(charges.findByIdForUpdate(2L)).thenReturn(Optional.of(debt));
        cycleService.cancel(4L, 99L); assertThat(debt.isCanceled()).isTrue(); verify(charges).save(debt);
        reset(charges, payments, refunds); var finished = cycle(); when(cycles.findById(4L)).thenReturn(Optional.of(finished)); when(cycles.save(finished)).thenReturn(finished);
        cycleService.finish(4L, 99L); cycleService.archive(4L, 99L);
        assertThat(finished.isArchived()).isTrue(); verifyNoInteractions(charges, payments, refunds);
    }
}
