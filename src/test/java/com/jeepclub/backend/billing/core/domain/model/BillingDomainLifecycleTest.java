package com.jeepclub.backend.billing.core.domain.model;

import com.jeepclub.backend.billing.core.domain.model.assignment.*;
import com.jeepclub.backend.billing.core.domain.enums.*;
import com.jeepclub.backend.billing.core.domain.enums.charge.*;
import com.jeepclub.backend.billing.core.domain.enums.cycle.*;
import com.jeepclub.backend.billing.core.domain.enums.definition.*;
import com.jeepclub.backend.billing.core.domain.enums.payment.*;
import com.jeepclub.backend.billing.core.domain.enums.refund.*;
import com.jeepclub.backend.billing.core.domain.exception.assignment.*;
import com.jeepclub.backend.billing.core.domain.exception.definition.*;
import com.jeepclub.backend.billing.core.domain.exception.cycle.*;
import com.jeepclub.backend.billing.core.domain.exception.charge.*;
import com.jeepclub.backend.billing.core.domain.exception.payment.*;
import com.jeepclub.backend.billing.core.domain.exception.refund.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.stream.Stream;
import static com.jeepclub.backend.billing.support.BillingFixtures.*;
import static org.assertj.core.api.Assertions.*;

class BillingDomainLifecycleTest {
    @Test void definitionUpdatesFutureConfigurationButCycleKeepsAllSnapshots() {
        var definition = definition(PaymentAcceptancePolicy.UNTIL_DAYS_AFTER_DUE_DATE, 3);
        var cycle = ChargeCycle.generate(definition, " code ", DUE, 99L, NOW);
        definition.deactivate(NOW.plusSeconds(1));
        definition.update(" new ", " new description ", BigDecimal.TEN, ChargeRecurrenceType.YEARLY,
                false, PaymentAcceptancePolicy.AFTER_DUE_DATE, null, NOW.plusSeconds(2));
        assertThat(definition.getName()).isEqualTo("new");
        assertThat(definition.getDescription()).isEqualTo("new description");
        assertThat(definition.getDefaultAmount()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(definition.getRecurrenceType()).isEqualTo(ChargeRecurrenceType.YEARLY);
        assertThat(definition.getRequired()).isFalse();
        assertThat(definition.getStatus()).isEqualTo(ChargeDefinitionStatus.INACTIVE);
        assertThat(definition.getUpdatedAt()).isEqualTo(NOW.plusSeconds(2));
        assertThat(cycle.getCode()).isEqualTo("code");
        assertThat(cycle.getChargeDefinitionNameSnapshot()).isEqualTo("monthly fee");
        assertThat(cycle.getChargeDefinitionDescriptionSnapshot()).isEqualTo("description");
        assertThat(cycle.getChargeDefinitionDefaultAmountSnapshot()).isEqualByComparingTo(AMOUNT);
        assertThat(cycle.getChargeDefinitionRecurrenceTypeSnapshot()).isEqualTo(ChargeRecurrenceType.ONE_TIME);
        assertThat(cycle.getChargeDefinitionRequiredSnapshot()).isTrue();
        assertThat(cycle.getChargeDefinitionPaymentAcceptancePolicySnapshot()).isEqualTo(PaymentAcceptancePolicy.UNTIL_DAYS_AFTER_DUE_DATE);
        assertThat(cycle.getChargeDefinitionLatePaymentGraceDaysSnapshot()).isEqualTo(3);
    }
    @Test void definitionActivationIsIdempotentAndArchivalIsTerminal() {
        var d = definition(); d.activate(NOW); d.activate(NOW); assertThat(d.isActive()).isTrue();
        d.deactivate(NOW); d.deactivate(NOW); assertThat(d.isActive()).isFalse();
        d.activate(NOW); d.archive(NOW.plusSeconds(1));
        assertThat(d.getArchivedAt()).isEqualTo(NOW.plusSeconds(1));
        assertThat(d.isActive()).isFalse();
        assertThatThrownBy(() -> d.activate(NOW)).isInstanceOf(ArchivedChargeDefinitionCannotBeActivatedException.class);
        assertThatThrownBy(() -> d.deactivate(NOW)).isInstanceOf(ArchivedChargeDefinitionCannotBeDeactivatedException.class);
        assertThatThrownBy(() -> d.archive(NOW)).isInstanceOf(ChargeDefinitionAlreadyArchivedException.class);
        assertThatThrownBy(() -> d.update("x", null, AMOUNT, ChargeRecurrenceType.ONE_TIME, true,
                PaymentAcceptancePolicy.UNTIL_DUE_DATE, null, NOW)).isInstanceOf(ArchivedChargeDefinitionCannotBeUpdatedException.class);
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {" ", "\t"})
    void definitionRejectsMissingName(String name) {
        assertThatThrownBy(() -> ChargeDefinition.create(name, null, AMOUNT, ChargeRecurrenceType.ONE_TIME,
                true, PaymentAcceptancePolicy.UNTIL_DUE_DATE, null, NOW)).isInstanceOf(IllegalArgumentException.class);
    }
    @ParameterizedTest @ValueSource(ints = {0, -1})
    void configurationAndDebtsRequirePositiveMoney(int value) {
        assertThatThrownBy(() -> ChargeDefinition.create("fee", null, BigDecimal.valueOf(value), ChargeRecurrenceType.ONE_TIME,
                true, PaymentAcceptancePolicy.UNTIL_DUE_DATE, null, NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MemberCharge.create(10L, 3L, 4L, BigDecimal.valueOf(value), DUE,
                PaymentAcceptancePolicy.UNTIL_DUE_DATE, null, NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MemberPayment.submitForValidation(2L, BigDecimal.valueOf(value), PaymentMethod.PIX,
                NOW, "key", null, NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MemberRefund.createMemberRequest(2L, 1L, 4L, 10L, BigDecimal.valueOf(value), 10L, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @ParameterizedTest @NullSource @ValueSource(ints = {0, -1})
    void gracePolicyRequiresPositiveDays(Integer days) {
        assertThatThrownBy(() -> definition(PaymentAcceptancePolicy.UNTIL_DAYS_AFTER_DUE_DATE, days)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> charge(PaymentAcceptancePolicy.UNTIL_DAYS_AFTER_DUE_DATE, days)).isInstanceOf(IllegalArgumentException.class);
    }
    @ParameterizedTest @EnumSource(value = PaymentAcceptancePolicy.class, names = {"UNTIL_DUE_DATE", "AFTER_DUE_DATE"})
    void otherPoliciesRejectGraceDays(PaymentAcceptancePolicy policy) {
        assertThatThrownBy(() -> definition(policy, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> charge(policy, 1)).isInstanceOf(IllegalArgumentException.class);
    }
    static Stream<ChargeAssignment> assignments() {
        return Stream.of(AllMembersChargeAssignment.create(3L, NOW), UserChargeAssignment.create(3L, 10L, NOW),
                RoleChargeAssignment.create(3L, 20L, NOW), EventParticipantsChargeAssignment.create(3L, 30L, NOW));
    }
    @ParameterizedTest @MethodSource("assignments")
    void assignmentTransitionsConflictOnRepetition(ChargeAssignment assignment) {
        assertThat(assignment.isActive()).isTrue();
        assertThatThrownBy(() -> assignment.activate(NOW)).isInstanceOf(ChargeAssignmentAlreadyActiveException.class);
        assignment.deactivate(NOW.plusSeconds(1)); assertThat(assignment.isActive()).isFalse();
        assertThat(assignment.getUpdatedAt()).isEqualTo(NOW.plusSeconds(1));
        assertThatThrownBy(() -> assignment.deactivate(NOW)).isInstanceOf(ChargeAssignmentAlreadyInactiveException.class);
        assignment.activate(NOW.plusSeconds(2)); assertThat(assignment.isActive()).isTrue();
        assertThat(assignment.getUpdatedAt()).isEqualTo(NOW.plusSeconds(2));
    }
    @ParameterizedTest @ValueSource(longs = {0, -1})
    void assignmentReferencesMustBePositive(long id) {
        assertThatThrownBy(() -> AllMembersChargeAssignment.create(id, NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> UserChargeAssignment.create(3L, id, NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RoleChargeAssignment.create(3L, id, NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EventParticipantsChargeAssignment.create(3L, id, NOW)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void cycleFinishAndArchivePreserveHistoricalActorAndTimes() {
        var c = cycle();
        assertThatThrownBy(() -> c.archive(99L, NOW)).isInstanceOf(ChargeCycleCannotBeArchivedException.class);
        c.finish(99L, NOW.plusSeconds(1));
        assertThat(c.isFinished()).isTrue(); assertThat(c.getFinishedAt()).isEqualTo(NOW.plusSeconds(1));
        assertThat(c.getFinishedByUserId()).isEqualTo(99L);
        assertThatThrownBy(() -> c.finish(99L, NOW)).isInstanceOf(ChargeCycleCannotBeFinishedException.class);
        assertThatThrownBy(() -> c.cancel(99L, NOW)).isInstanceOf(ChargeCycleCannotBeCanceledException.class);
        c.archive(98L, NOW.plusSeconds(2));
        assertThat(c.isArchived()).isTrue(); assertThat(c.getArchivedByUserId()).isEqualTo(98L);
        assertThat(c.getArchivedAt()).isEqualTo(NOW.plusSeconds(2)); assertThat(c.getFinishedAt()).isEqualTo(NOW.plusSeconds(1));
        assertThatThrownBy(() -> c.archive(99L, NOW)).isInstanceOf(ChargeCycleCannotBeArchivedException.class);
    }
    @Test void cycleCancellationCanOnlyBeArchivedAfterwards() {
        var c = cycle(); c.cancel(99L, NOW.plusSeconds(1));
        assertThat(c.isCanceled()).isTrue(); assertThat(c.getCanceledAt()).isEqualTo(NOW.plusSeconds(1));
        assertThat(c.getCanceledByUserId()).isEqualTo(99L);
        assertThatThrownBy(() -> c.cancel(99L, NOW)).isInstanceOf(ChargeCycleAlreadyCanceledException.class);
        assertThatThrownBy(() -> c.finish(99L, NOW)).isInstanceOf(ChargeCycleCannotBeFinishedException.class);
        c.archive(98L, NOW.plusSeconds(2)); assertThat(c.isArchived()).isTrue();
        assertThat(c.getCanceledAt()).isEqualTo(NOW.plusSeconds(1));
    }
    @Test void chargeGraceWindowIsInclusiveAndEffectiveStatusesAreNeverPersisted() {
        var c = charge(PaymentAcceptancePolicy.UNTIL_DAYS_AFTER_DUE_DATE, 2);
        assertThat(c.getPaymentAllowedUntil()).isEqualTo(DUE.plusDays(2));
        assertThat(c.effectiveStatusAt(DUE)).isEqualTo(MemberChargeEffectiveStatus.PENDING);
        assertThat(c.effectiveStatusAt(DUE.plusDays(1))).isEqualTo(MemberChargeEffectiveStatus.OVERDUE);
        assertThat(c.acceptsPaymentOn(DUE.plusDays(2))).isTrue();
        assertThat(c.effectiveStatusAt(DUE.plusDays(3))).isEqualTo(MemberChargeEffectiveStatus.EXPIRED);
        assertThat(c.acceptsPaymentOn(DUE.plusDays(3))).isFalse();
        assertThat(c.getStatus()).isEqualTo(MemberChargeStatus.PENDING);
        assertThat(c.isDue(DUE)).isFalse(); assertThat(c.isDue(DUE.plusDays(1))).isTrue();
    }
    @Test void unrestrictedChargeRemainsPayableBeforeAndAfterDueDate() {
        var c = charge(PaymentAcceptancePolicy.AFTER_DUE_DATE, null);
        assertThat(c.getPaymentAllowedUntil()).isNull();
        assertThat(c.acceptsPaymentOn(DUE.minusDays(1))).isTrue();
        assertThat(c.effectiveStatusAt(DUE.plusYears(1))).isEqualTo(MemberChargeEffectiveStatus.OVERDUE);
        assertThat(c.acceptsPaymentOn(DUE.plusYears(1))).isTrue();
    }
    @Test void finalAmountMayDecreaseAndRestoreButNotExceedOriginalOrChangeAfterExpiry() {
        var c = charge(); c.updateFinalAmount(BigDecimal.TEN, DUE, NOW);
        assertThat(c.getFinalAmount()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(c.getOriginalAmount()).isEqualByComparingTo(AMOUNT);
        c.updateFinalAmount(AMOUNT, DUE, NOW.plusSeconds(1));
        assertThat(c.getFinalAmount()).isEqualByComparingTo(AMOUNT);
        assertThat(c.getUpdatedAt()).isEqualTo(NOW.plusSeconds(1));
        assertThatThrownBy(() -> c.updateFinalAmount(AMOUNT.add(BigDecimal.ONE), DUE, NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> c.updateFinalAmount(BigDecimal.ZERO, DUE, NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> c.updateFinalAmount(BigDecimal.TEN, DUE.plusDays(1), NOW)).isInstanceOf(InvalidMemberChargeStateException.class);
    }
    @ParameterizedTest @ValueSource(booleans = {true, false})
    void closedChargeRejectsFurtherFinancialMutation(boolean paid) {
        var c = charge(); if (paid) c.markAsPaid(NOW.minusSeconds(60), NOW); else c.cancel(NOW);
        assertThat(c.effectiveStatusAt(DUE.plusYears(1))).isEqualTo(paid ? MemberChargeEffectiveStatus.PAID : MemberChargeEffectiveStatus.CANCELED);
        assertThat(c.acceptsPaymentOn(DUE)).isFalse(); assertThat(c.isDue(DUE.plusDays(1))).isFalse();
        assertThatThrownBy(() -> c.markAsPaid(NOW, NOW)).isInstanceOf(InvalidMemberChargeStateException.class);
        assertThatThrownBy(() -> c.cancel(NOW)).isInstanceOf(InvalidMemberChargeStateException.class);
        assertThatThrownBy(() -> c.updateFinalAmount(BigDecimal.TEN, DUE, NOW)).isInstanceOf(InvalidMemberChargeStateException.class);
    }
    @Test void rejectionAndResubmissionClearReviewAndAdvanceSubmissionTime() {
        var p = payment(); p.reject(99L, " invalid receipt ", NOW.plusSeconds(1));
        assertThat(p.isRejected()).isTrue(); assertThat(p.getRejectionReason()).isEqualTo("invalid receipt");
        assertThat(p.getRejectedAt()).isEqualTo(NOW.plusSeconds(1)); assertThat(p.getRejectedByUserId()).isEqualTo(99L);
        assertThat(p.getSubmittedAt()).isEqualTo(NOW);
        assertThatThrownBy(() -> p.confirm(99L, NOW)).isInstanceOf(InvalidMemberPaymentStateException.class);
        p.updateSubmission(BigDecimal.TEN, PaymentMethod.PIX, NOW, " new-key ", " new notes ", NOW.plusSeconds(2));
        assertThat(p.isPendingValidation()).isTrue(); assertThat(p.getSubmittedAt()).isEqualTo(NOW.plusSeconds(2));
        assertThat(p.getCreatedAt()).isEqualTo(NOW); assertThat(p.getReceiptStorageKey()).isEqualTo("new-key");
        assertThat(p.getAmount()).isEqualByComparingTo(BigDecimal.TEN); assertThat(p.getNotes()).isEqualTo("new notes");
        assertThat(p.getRejectedAt()).isNull(); assertThat(p.getRejectedByUserId()).isNull(); assertThat(p.getRejectionReason()).isNull();
    }
    @Test void confirmationPreservesSubmissionTimeAndIsTerminal() {
        var p = payment(); p.confirm(99L, NOW.plusSeconds(2));
        assertThat(p.isConfirmed()).isTrue(); assertThat(p.getConfirmedByUserId()).isEqualTo(99L);
        assertThat(p.getConfirmedAt()).isEqualTo(NOW.plusSeconds(2)); assertThat(p.getUpdatedAt()).isEqualTo(NOW.plusSeconds(2));
        assertThat(p.getSubmittedAt()).isEqualTo(NOW);
        assertThatThrownBy(() -> p.confirm(99L, NOW)).isInstanceOf(InvalidMemberPaymentStateException.class);
        assertThatThrownBy(() -> p.reject(99L, "x", NOW)).isInstanceOf(InvalidMemberPaymentStateException.class);
        assertThatThrownBy(() -> p.cancel(NOW)).isInstanceOf(InvalidMemberPaymentStateException.class);
        assertThatThrownBy(() -> p.updateSubmission(AMOUNT, PaymentMethod.PIX, NOW, "key", null, NOW)).isInstanceOf(InvalidMemberPaymentStateException.class);
    }
    @Test void internalPaymentCancellationClosesPendingSubmission() {
        var p = payment(); p.cancel(NOW.plusSeconds(1));
        assertThat(p.isCanceled()).isTrue(); assertThat(p.getCanceledAt()).isEqualTo(NOW.plusSeconds(1));
        assertThatThrownBy(() -> p.cancel(NOW)).isInstanceOf(InvalidMemberPaymentStateException.class);
        assertThatThrownBy(() -> p.confirm(99L, NOW)).isInstanceOf(InvalidMemberPaymentStateException.class);
        assertThatThrownBy(() -> p.updateSubmission(AMOUNT, PaymentMethod.PIX, NOW, "key", null, NOW)).isInstanceOf(InvalidMemberPaymentStateException.class);
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {" "})
    void rejectionRequiresReason(String reason) {
        assertThatThrownBy(() -> payment().reject(99L, reason, NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> request().reject(99L, reason, NOW)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void refundRequestApprovalAndSettlementKeepActorsAndMoney() {
        var r = eligibility(); r.request(10L, NOW.plusSeconds(1));
        assertThat(r.isRequested()).isTrue(); assertThat(r.getRequestedAt()).isEqualTo(NOW.plusSeconds(1));
        assertThat(r.getRequestedByUserId()).isEqualTo(10L);
        r.approve(99L, NOW.plusSeconds(2)); assertThat(r.isApproved()).isTrue();
        assertThat(r.getApprovedAt()).isEqualTo(NOW.plusSeconds(2)); assertThat(r.getApprovedByUserId()).isEqualTo(99L);
        r.markAsRefunded(98L, NOW.plusSeconds(3)); assertThat(r.isRefunded()).isTrue();
        assertThat(r.getRefundedAt()).isEqualTo(NOW.plusSeconds(3)); assertThat(r.getRefundedByUserId()).isEqualTo(98L);
        assertThat(r.getAmount()).isEqualByComparingTo(AMOUNT); assertThat(r.isActive()).isFalse();
        assertThatThrownBy(() -> r.cancel(99L, NOW)).isInstanceOf(InvalidMemberRefundStateException.class);
        assertThatThrownBy(() -> r.approve(99L, NOW)).isInstanceOf(InvalidMemberRefundStateException.class);
        assertThatThrownBy(() -> r.markAsRefunded(99L, NOW)).isInstanceOf(InvalidMemberRefundStateException.class);
    }
    @Test void eligibilityExpiresAtExactInstantAndOnlyEligibleRefundCanExpire() {
        var until = NOW.plus(Duration.ofDays(30)); var r = eligibility();
        assertThat(r.isEligibilityExpiredAt(until.minusNanos(1))).isFalse();
        assertThat(r.isEligibilityExpiredAt(until)).isTrue();
        assertThatThrownBy(() -> r.request(10L, until)).isInstanceOf(InvalidMemberRefundStateException.class);
        assertThatThrownBy(() -> r.approve(99L, until)).isInstanceOf(InvalidMemberRefundStateException.class);
        assertThatThrownBy(() -> r.expire(until.minusNanos(1))).isInstanceOf(InvalidMemberRefundStateException.class);
        r.expire(until); assertThat(r.isExpired()).isTrue(); assertThat(r.getUpdatedAt()).isEqualTo(until);
        assertThatThrownBy(() -> r.expire(until)).isInstanceOf(InvalidMemberRefundStateException.class);
        assertThatThrownBy(() -> request().expire(until)).isInstanceOf(InvalidMemberRefundStateException.class);
    }
    @Test void requestedRefundCanBeApprovedAfterEligibilityWindowAndDirectRequestHasNoExpiry() {
        var r = eligibility(); r.request(10L, NOW); r.approve(99L, NOW.plus(Duration.ofDays(31)));
        assertThat(r.isApproved()).isTrue();
        assertThat(request().isEligibilityExpiredAt(NOW.plus(Duration.ofDays(100)))).isFalse();
        var direct = eligibility(); direct.approve(99L, NOW); assertThat(direct.isApproved()).isTrue();
    }
    @Test void refundRejectionIsTerminalAndCarriesNormalizedReason() {
        var r = request(); r.reject(99L, " refused ", NOW.plusSeconds(1));
        assertThat(r.getStatus()).isEqualTo(MemberRefundStatus.REJECTED); assertThat(r.isActive()).isFalse();
        assertThat(r.getRejectedByUserId()).isEqualTo(99L); assertThat(r.getRejectedAt()).isEqualTo(NOW.plusSeconds(1));
        assertThat(r.getRejectionReason()).isEqualTo("refused");
        assertThatThrownBy(() -> r.reject(99L, "x", NOW)).isInstanceOf(InvalidMemberRefundStateException.class);
        assertThatThrownBy(() -> r.cancel(99L, NOW)).isInstanceOf(InvalidMemberRefundStateException.class);
        assertThatThrownBy(() -> eligibility().reject(99L, "x", NOW)).isInstanceOf(InvalidMemberRefundStateException.class);
    }
    @ParameterizedTest @ValueSource(ints = {0, 1, 2})
    void allActiveRefundStatesCanBeCanceled(int stage) {
        var r = eligibility(); if (stage >= 1) r.request(10L, NOW); if (stage == 2) r.approve(99L, NOW);
        assertThat(r.isActive()).isTrue(); r.cancel(98L, NOW.plusSeconds(1));
        assertThat(r.isCanceled()).isTrue(); assertThat(r.isActive()).isFalse();
        assertThat(r.getCanceledByUserId()).isEqualTo(98L); assertThat(r.getCanceledAt()).isEqualTo(NOW.plusSeconds(1));
        assertThatThrownBy(() -> r.request(10L, NOW)).isInstanceOf(InvalidMemberRefundStateException.class);
        assertThatThrownBy(() -> r.markAsRefunded(99L, NOW)).isInstanceOf(InvalidMemberRefundStateException.class);
    }
}
