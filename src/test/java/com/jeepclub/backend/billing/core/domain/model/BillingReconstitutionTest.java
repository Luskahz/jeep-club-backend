package com.jeepclub.backend.billing.core.domain.model;

import com.jeepclub.backend.billing.core.domain.enums.*;
import com.jeepclub.backend.billing.core.domain.enums.charge.*;
import com.jeepclub.backend.billing.core.domain.enums.cycle.*;
import com.jeepclub.backend.billing.core.domain.enums.definition.*;
import com.jeepclub.backend.billing.core.domain.enums.payment.*;
import com.jeepclub.backend.billing.core.domain.enums.refund.*;
import com.jeepclub.backend.billing.infra.persistence.mapper.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.math.BigDecimal;
import static com.jeepclub.backend.billing.support.BillingFixtures.*;
import static org.assertj.core.api.Assertions.*;

class BillingReconstitutionTest {
    @ParameterizedTest @ValueSource(ints = {0, 1, 2, 3, 4, 5})
    void persistedChargeCannotHaveContradictoryMoneyWindowOrTerminalDates(int corruption) {
        assertThatThrownBy(() -> MemberCharge.reconstitute(2L, 10L, 3L, 4L, AMOUNT, corruption == 0 ? AMOUNT.add(BigDecimal.ONE) : AMOUNT,
                DUE, PaymentAcceptancePolicy.UNTIL_DUE_DATE, null, corruption == 1 ? DUE.plusDays(1) : DUE,
                corruption == 2 ? MemberChargeStatus.PAID : corruption == 3 ? MemberChargeStatus.CANCELED : MemberChargeStatus.PENDING,
                NOW, null, corruption == 4 ? NOW : null, corruption == 5 ? NOW : null)).isInstanceOf(IllegalArgumentException.class);
    }
    @ParameterizedTest @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8})
    void persistedPaymentRequiresExactlyTheMetadataOfItsState(int corruption) {
        var state = corruption <= 1 ? MemberPaymentStatus.CONFIRMED : corruption <= 4 ? MemberPaymentStatus.REJECTED : corruption == 5 ? MemberPaymentStatus.CANCELED : MemberPaymentStatus.PENDING_VALIDATION;
        assertThatThrownBy(() -> MemberPayment.reconstitute(1L, 2L, AMOUNT, PaymentMethod.PIX, state, NOW, "key",
                corruption == 1 || corruption == 6 ? NOW : null, corruption == 0 ? 99L : null,
                corruption == 3 || corruption == 4 || corruption == 7 ? NOW : null, corruption == 2 || corruption == 4 ? 99L : null,
                corruption == 2 || corruption == 3 ? "reason" : null, corruption == 8 ? NOW : null, null, NOW, null))
                .isInstanceOfAny(IllegalArgumentException.class, NullPointerException.class);
    }
    @ParameterizedTest @ValueSource(ints = {0, 1})
    void persistedDefinitionArchiveDateMustMatchStatus(int corruption) {
        assertThatThrownBy(() -> ChargeDefinition.reconstitute(3L, "fee", null, AMOUNT, ChargeRecurrenceType.ONE_TIME, true,
                PaymentAcceptancePolicy.UNTIL_DUE_DATE, null, corruption == 0 ? ChargeDefinitionStatus.ARCHIVED : ChargeDefinitionStatus.ACTIVE,
                NOW, null, corruption == 0 ? null : NOW)).isInstanceOf(IllegalArgumentException.class);
    }
    @ParameterizedTest @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8})
    void cycleHistoricalMetadataMustMatchTerminalSource(int corruption) {
        var status = corruption <= 2 ? ChargeCycleStatus.GENERATED : corruption == 3 ? ChargeCycleStatus.CANCELED : corruption == 4 ? ChargeCycleStatus.FINISHED : ChargeCycleStatus.ARCHIVED;
        assertThatThrownBy(() -> ChargeCycle.reconstitute(4L, 3L, "fee", null, AMOUNT, ChargeRecurrenceType.ONE_TIME, true,
                PaymentAcceptancePolicy.UNTIL_DUE_DATE, null, "code", DUE, status, 99L, NOW,
                corruption == 0 || corruption == 7 ? NOW : null, corruption == 0 || corruption == 7 ? 99L : null,
                corruption == 1 || corruption == 7 ? NOW : null, corruption == 1 || corruption == 7 ? 99L : null,
                corruption == 2 || corruption >= 6 ? NOW : null, corruption == 2 || corruption >= 6 ? 99L : null, NOW, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @ParameterizedTest @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11})
    void refundsRejectMissingReviewMetadataAndContradictoryEligibility(int corruption) {
        var status = switch (corruption) { case 0, 1, 2, 3 -> MemberRefundStatus.ELIGIBLE; case 4 -> MemberRefundStatus.REQUESTED;
            case 5 -> MemberRefundStatus.APPROVED; case 6, 7 -> MemberRefundStatus.REJECTED; case 8 -> MemberRefundStatus.REFUNDED;
            case 9 -> MemberRefundStatus.CANCELED; case 10 -> MemberRefundStatus.EXPIRED; default -> MemberRefundStatus.ELIGIBLE; };
        assertThatThrownBy(() -> MemberRefund.reconstitute(1L, 2L, 3L, 4L, 10L, AMOUNT, RefundReason.CYCLE_CANCELED_BY_ADMIN,
                status, corruption == 0 ? null : NOW, corruption == 1 ? null : corruption == 2 ? NOW : NOW.plusSeconds(100), 99L,
                corruption == 3 || corruption == 10 ? NOW : null, corruption == 3 || corruption == 10 ? 10L : null, corruption == 11 ? NOW : null, corruption == 11 ? 99L : null,
                corruption == 7 ? NOW : null, corruption == 7 ? 99L : null, null, null, null, null, null, NOW, null))
                .isInstanceOfAny(IllegalArgumentException.class, NullPointerException.class);
    }
    @Test void historicalTerminalStatesRemainReadableThroughPersistenceMappers() {
        var c = charge(); c.markAsPaid(NOW.minusSeconds(60), NOW);
        assertThat(new MemberChargeMapper().toDomain(new MemberChargeMapper().toEntity(c))).usingRecursiveComparison().isEqualTo(c);
        var p = payment(); p.reject(99L, "reason", NOW);
        assertThat(new MemberPaymentMapper().toDomain(new MemberPaymentMapper().toEntity(p))).usingRecursiveComparison().isEqualTo(p);
        var cycle = cycle(); cycle.finish(99L, NOW); cycle.archive(99L, NOW);
        assertThat(new ChargeCycleMapper().toDomain(new ChargeCycleMapper().toEntity(cycle))).usingRecursiveComparison().isEqualTo(cycle);
    }
    @Test void unusedRejectedPaymentCancellationCurrentlyCannotBeReconstituted() {
        // Audit finding: no application service calls cancel; retain current behavior pending a lifecycle decision.
        var p = payment(); p.reject(99L, "reason", NOW); p.cancel(NOW.plusSeconds(1));
        assertThat(p.isCanceled()).isTrue(); assertThat(p.getRejectionReason()).isEqualTo("reason");
        var mapper = new MemberPaymentMapper();
        assertThatThrownBy(() -> mapper.toDomain(mapper.toEntity(p))).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("rejectedAt must be null when payment is not rejected.");
    }
}
