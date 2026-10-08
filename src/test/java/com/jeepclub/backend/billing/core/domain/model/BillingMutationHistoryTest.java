package com.jeepclub.backend.billing.core.domain.model;

import com.jeepclub.backend.billing.core.domain.enums.cycle.*;
import com.jeepclub.backend.billing.core.domain.enums.refund.*;
import com.jeepclub.backend.billing.infra.persistence.mapper.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.stream.Stream;
import static com.jeepclub.backend.billing.support.BillingFixtures.*;
import static org.assertj.core.api.Assertions.*;

class BillingMutationHistoryTest {
    private static MemberRefund refund(MemberRefundStatus status) {
        var r = eligibility();
        switch (status) {
            case REQUESTED -> r.request(10L, NOW);
            case APPROVED -> { r.request(10L, NOW); r.approve(99L, NOW); }
            case REJECTED -> { r.request(10L, NOW); r.reject(99L, "reason", NOW); }
            case REFUNDED -> { r.request(10L, NOW); r.approve(99L, NOW); r.markAsRefunded(99L, NOW); }
            case EXPIRED -> r.expire(NOW.plusSeconds(30 * 86400));
            case CANCELED -> r.cancel(99L, NOW);
            default -> { }
        }
        return r;
    }

    static Stream<Arguments> contradictoryRefundHistory() {
        return Stream.of(
            Arguments.of(MemberRefundStatus.ELIGIBLE, "rejectedAt"),
            Arguments.of(MemberRefundStatus.ELIGIBLE, "refundedAt"),
            Arguments.of(MemberRefundStatus.ELIGIBLE, "canceledAt"),
            Arguments.of(MemberRefundStatus.REQUESTED, "approvedAt"),
            Arguments.of(MemberRefundStatus.REQUESTED, "rejectedAt"),
            Arguments.of(MemberRefundStatus.REQUESTED, "refundedAt"),
            Arguments.of(MemberRefundStatus.REQUESTED, "canceledAt"),
            Arguments.of(MemberRefundStatus.APPROVED, "rejectedAt"),
            Arguments.of(MemberRefundStatus.APPROVED, "refundedAt"),
            Arguments.of(MemberRefundStatus.APPROVED, "canceledAt"),
            Arguments.of(MemberRefundStatus.REJECTED, "approvedAt"),
            Arguments.of(MemberRefundStatus.REJECTED, "refundedAt"),
            Arguments.of(MemberRefundStatus.REJECTED, "canceledAt"),
            Arguments.of(MemberRefundStatus.REFUNDED, "rejectedAt"),
            Arguments.of(MemberRefundStatus.REFUNDED, "canceledAt"),
            Arguments.of(MemberRefundStatus.EXPIRED, "approvedAt"),
            Arguments.of(MemberRefundStatus.EXPIRED, "rejectedAt"),
            Arguments.of(MemberRefundStatus.EXPIRED, "refundedAt"),
            Arguments.of(MemberRefundStatus.EXPIRED, "canceledAt"),
            Arguments.of(MemberRefundStatus.CANCELED, "rejectedAt"),
            Arguments.of(MemberRefundStatus.CANCELED, "refundedAt")
        );
    }
    @ParameterizedTest @MethodSource("contradictoryRefundHistory")
    void refundHistoryRejectsASecondIncompatibleDecision(MemberRefundStatus status, String field) {
        var mapper = new MemberRefundMapper();
        var entity = mapper.toEntity(refund(status));
        // Corrupt only the persisted fixture; exercise the public reconstruction boundary.
        ReflectionTestUtils.setField(entity, field, NOW);
        assertThatThrownBy(() -> mapper.toDomain(entity)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest @EnumSource(MemberRefundStatus.class)
    void persistedRefundKeepsItsDecisionAndPublicState(MemberRefundStatus status) {
        var mapper = new MemberRefundMapper(); var original = refund(status);
        var restored = mapper.toDomain(mapper.toEntity(original));
        assertThat(restored).usingRecursiveComparison().isEqualTo(original);
        assertThat(restored.isRequested()).isEqualTo(status == MemberRefundStatus.REQUESTED);
        assertThat(restored.isApproved()).isEqualTo(status == MemberRefundStatus.APPROVED);
        assertThat(restored.isRefunded()).isEqualTo(status == MemberRefundStatus.REFUNDED);
        assertThat(restored.isExpired()).isEqualTo(status == MemberRefundStatus.EXPIRED);
        assertThat(restored.isCanceled()).isEqualTo(status == MemberRefundStatus.CANCELED);
    }
    @ParameterizedTest @ValueSource(strings = {"approvedAt", "refundedAt"})
    void refundedHistoryRequiresBothApprovalAndTransfer(String field) {
        var mapper = new MemberRefundMapper(); var entity = mapper.toEntity(refund(MemberRefundStatus.REFUNDED));
        ReflectionTestUtils.setField(entity, field, null);
        assertThatThrownBy(() -> mapper.toDomain(entity)).isInstanceOf(IllegalArgumentException.class);
    }
    @ParameterizedTest @EnumSource(value = MemberRefundStatus.class, names = {"ELIGIBLE", "EXPIRED"})
    void eligibilityStatesRequireAWindowEvenForNonCycleReasons(MemberRefundStatus status) {
        var mapper = new MemberRefundMapper(); var entity = mapper.toEntity(refund(status));
        ReflectionTestUtils.setField(entity, "reason", RefundReason.MANUAL_ADJUSTMENT);
        ReflectionTestUtils.setField(entity, "eligibleAt", null);
        ReflectionTestUtils.setField(entity, "eligibleUntil", null);
        assertThatThrownBy(() -> mapper.toDomain(entity)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void cycleCancellationReasonRequiresAWindowForRequestedRefunds() {
        var mapper = new MemberRefundMapper(); var entity = mapper.toEntity(request());
        ReflectionTestUtils.setField(entity, "reason", RefundReason.CYCLE_CANCELED_BY_ADMIN);
        assertThatThrownBy(() -> mapper.toDomain(entity)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void optionalHistoricalActorsCannotBeZero() {
        var mapper = new MemberRefundMapper(); var entity = mapper.toEntity(refund(MemberRefundStatus.APPROVED));
        ReflectionTestUtils.setField(entity, "requestedByUserId", 0L);
        assertThatThrownBy(() -> mapper.toDomain(entity)).isInstanceOf(IllegalArgumentException.class);
        var direct = mapper.toEntity(request()); ReflectionTestUtils.setField(direct, "createdByUserId", 0L);
        assertThatThrownBy(() -> mapper.toDomain(direct)).isInstanceOf(IllegalArgumentException.class);
    }

    private static ChargeCycle historicalCycle(ChargeCycleStatus status) {
        var c = cycle();
        if (status == ChargeCycleStatus.CANCELED) c.cancel(99L, NOW);
        if (status == ChargeCycleStatus.FINISHED || status == ChargeCycleStatus.ARCHIVED) c.finish(99L, NOW);
        if (status == ChargeCycleStatus.ARCHIVED) c.archive(99L, NOW);
        return c;
    }
    @ParameterizedTest @EnumSource(ChargeCycleStatus.class)
    void persistedCycleKeepsTerminalHistoryAndState(ChargeCycleStatus status) {
        var mapper = new ChargeCycleMapper(); var original = historicalCycle(status);
        var restored = mapper.toDomain(mapper.toEntity(original));
        assertThat(restored).usingRecursiveComparison().isEqualTo(original);
        assertThat(restored.isCanceled()).isEqualTo(status == ChargeCycleStatus.CANCELED);
        assertThat(restored.isFinished()).isEqualTo(status == ChargeCycleStatus.FINISHED);
        assertThat(restored.isArchived()).isEqualTo(status == ChargeCycleStatus.ARCHIVED);
    }
    @ParameterizedTest @CsvSource({"CANCELED,finishedAt", "CANCELED,archivedAt", "FINISHED,canceledAt", "FINISHED,archivedAt"})
    void cycleHistoryCannotMixTerminalDecisions(ChargeCycleStatus status, String field) {
        var mapper = new ChargeCycleMapper(); var entity = mapper.toEntity(historicalCycle(status));
        ReflectionTestUtils.setField(entity, field, NOW);
        assertThatThrownBy(() -> mapper.toDomain(entity)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void archivedCycleRequiresItsOwnArchiveTimestamp() {
        var mapper = new ChargeCycleMapper(); var entity = mapper.toEntity(historicalCycle(ChargeCycleStatus.ARCHIVED));
        ReflectionTestUtils.setField(entity, "archivedAt", null);
        assertThatThrownBy(() -> mapper.toDomain(entity)).isInstanceOf(IllegalArgumentException.class);
    }
    @ParameterizedTest @ValueSource(strings = {"chargeDefinitionId", "generatedByUserId", "chargeDefinitionDefaultAmountSnapshot", "chargeDefinitionLatePaymentGraceDaysSnapshot"})
    void persistedCycleRejectsZeroFinancialConfiguration(String field) {
        var mapper = new ChargeCycleMapper(); var entity = mapper.toEntity(cycle());
        Object value = Long.valueOf(0);
        if (field.equals("chargeDefinitionDefaultAmountSnapshot")) value = BigDecimal.ZERO;
        if (field.equals("chargeDefinitionLatePaymentGraceDaysSnapshot")) value = Integer.valueOf(0);
        if (field.equals("chargeDefinitionLatePaymentGraceDaysSnapshot")) {
            ReflectionTestUtils.setField(entity, "chargeDefinitionPaymentAcceptancePolicySnapshot", PaymentAcceptancePolicy.UNTIL_DAYS_AFTER_DUE_DATE);
        }
        ReflectionTestUtils.setField(entity, field, value);
        assertThatThrownBy(() -> mapper.toDomain(entity)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void missingCycleGenerationTimeIsRejected() {
        var mapper = new ChargeCycleMapper(); var entity = mapper.toEntity(cycle());
        ReflectionTestUtils.setField(entity, "generatedAt", null);
        assertThatThrownBy(() -> mapper.toDomain(entity)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void definitionUpdateCannotIntroduceAnInvalidPaymentWindow() {
        assertThatThrownBy(() -> definition().update("fee", null, AMOUNT,
                com.jeepclub.backend.billing.core.domain.enums.ChargeRecurrenceType.ONE_TIME, true,
                PaymentAcceptancePolicy.UNTIL_DAYS_AFTER_DUE_DATE, 0, NOW)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void persistedPaymentPreservesSubmissionTimeAndUsesCreationTimeForLegacyRows() {
        var mapper = new MemberPaymentMapper(); var entity = mapper.toEntity(payment());
        ReflectionTestUtils.setField(entity, "submittedAt", NOW.plusSeconds(60));
        assertThat(mapper.toDomain(entity).getSubmittedAt()).isEqualTo(NOW.plusSeconds(60));
        ReflectionTestUtils.setField(entity, "submittedAt", null);
        assertThat(mapper.toDomain(entity).getSubmittedAt()).isEqualTo(payment().getCreatedAt());
    }
    @Test void optionalDescriptionsRemainAbsentInConfigurationAndSnapshots() {
        var d = definition();
        d.update("fee", "  ", AMOUNT, com.jeepclub.backend.billing.core.domain.enums.ChargeRecurrenceType.ONE_TIME,
                true, PaymentAcceptancePolicy.UNTIL_DUE_DATE, null, NOW);
        assertThat(d.getDescription()).isNull();
        var c = ChargeCycle.generate(d, "code", DUE, 99L, NOW);
        var mapper = new ChargeCycleMapper();
        assertThat(mapper.toDomain(mapper.toEntity(c)).getChargeDefinitionDescriptionSnapshot()).isNull();
    }
    @ParameterizedTest @ValueSource(strings = {"canceledByUserId", "finishedByUserId", "archivedByUserId"})
    void archivedCyclePreservesOnlyPositiveHistoricalActors(String field) {
        var mapper = new ChargeCycleMapper();
        var c = cycle();
        if (field.equals("canceledByUserId")) c.cancel(99L, NOW); else c.finish(99L, NOW);
        c.archive(99L, NOW);
        var entity = mapper.toEntity(c); ReflectionTestUtils.setField(entity, field, 0L);
        assertThatThrownBy(() -> mapper.toDomain(entity)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void canceledRefundPreservesOnlyPositiveApprovalActors() {
        var mapper = new MemberRefundMapper(); var r = refund(MemberRefundStatus.APPROVED); r.cancel(99L, NOW);
        var entity = mapper.toEntity(r); ReflectionTestUtils.setField(entity, "approvedByUserId", 0L);
        assertThatThrownBy(() -> mapper.toDomain(entity)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void chargeOwnerAndPaymentChargeReferenceCannotBeZero() {
        var chargeMapper = new MemberChargeMapper(); var chargeEntity = chargeMapper.toEntity(charge());
        ReflectionTestUtils.setField(chargeEntity, "userId", 0L);
        assertThatThrownBy(() -> chargeMapper.toDomain(chargeEntity)).isInstanceOf(IllegalArgumentException.class);
        var paymentMapper = new MemberPaymentMapper(); var paymentEntity = paymentMapper.toEntity(payment());
        ReflectionTestUtils.setField(paymentEntity, "memberChargeId", 0L);
        assertThatThrownBy(() -> paymentMapper.toDomain(paymentEntity)).isInstanceOf(IllegalArgumentException.class);
    }
    @ParameterizedTest @EnumSource(com.jeepclub.backend.billing.core.domain.enums.payment.MemberPaymentStatus.class)
    void persistedPaymentStateAgreesWithReviewAndCancellation(com.jeepclub.backend.billing.core.domain.enums.payment.MemberPaymentStatus status) {
        var p = payment();
        switch (status) {
            case CONFIRMED -> p.confirm(99L, NOW);
            case REJECTED -> p.reject(99L, "reason", NOW);
            case CANCELED -> p.cancel(NOW);
            default -> { }
        }
        var mapper = new MemberPaymentMapper(); var restored = mapper.toDomain(mapper.toEntity(p));
        assertThat(restored).usingRecursiveComparison().isEqualTo(p);
        assertThat(restored.isPendingValidation()).isEqualTo(status == com.jeepclub.backend.billing.core.domain.enums.payment.MemberPaymentStatus.PENDING_VALIDATION);
        assertThat(restored.isCanceled()).isEqualTo(status == com.jeepclub.backend.billing.core.domain.enums.payment.MemberPaymentStatus.CANCELED);
    }
}
