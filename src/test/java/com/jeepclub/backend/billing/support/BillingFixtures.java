package com.jeepclub.backend.billing.support;

import com.jeepclub.backend.billing.core.domain.model.*;
import com.jeepclub.backend.billing.core.domain.enums.ChargeRecurrenceType;
import com.jeepclub.backend.billing.core.domain.enums.cycle.PaymentAcceptancePolicy;
import com.jeepclub.backend.billing.core.domain.enums.definition.ChargeDefinitionStatus;
import com.jeepclub.backend.billing.core.domain.enums.charge.MemberChargeStatus;
import com.jeepclub.backend.billing.core.domain.enums.payment.*;
import com.jeepclub.backend.billing.infra.persistence.mapper.*;
import java.math.BigDecimal;
import java.time.*;

public final class BillingFixtures {
    public static final Instant NOW = Instant.parse("2026-09-15T12:00:00Z");
    public static final LocalDate DUE = LocalDate.of(2026, 9, 15);
    public static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    public static final BigDecimal AMOUNT = new BigDecimal("100.00");
    private BillingFixtures() {}
    public static ChargeDefinition definition() {
        return definition(PaymentAcceptancePolicy.UNTIL_DUE_DATE, null);
    }
    public static ChargeDefinition definition(PaymentAcceptancePolicy policy, Integer grace) {
        return ChargeDefinition.reconstitute(3L, "monthly fee", "description", AMOUNT,
                ChargeRecurrenceType.ONE_TIME, true, policy, grace, ChargeDefinitionStatus.ACTIVE, NOW, null, null);
    }
    public static ChargeCycle cycle() {
        return withId(ChargeCycle.generate(definition(), "2026-09", DUE, 99L, NOW), 4L);
    }
    public static ChargeCycle withId(ChargeCycle cycle, Long id) {
        return ChargeCycle.reconstitute(id, cycle.getChargeDefinitionId(), cycle.getChargeDefinitionNameSnapshot(),
                cycle.getChargeDefinitionDescriptionSnapshot(), cycle.getChargeDefinitionDefaultAmountSnapshot(),
                cycle.getChargeDefinitionRecurrenceTypeSnapshot(), cycle.getChargeDefinitionRequiredSnapshot(),
                cycle.getChargeDefinitionPaymentAcceptancePolicySnapshot(), cycle.getChargeDefinitionLatePaymentGraceDaysSnapshot(),
                cycle.getCode(), cycle.getDueDate(), cycle.getStatus(), cycle.getGeneratedByUserId(), cycle.getGeneratedAt(),
                cycle.getCanceledAt(), cycle.getCanceledByUserId(), cycle.getFinishedAt(), cycle.getFinishedByUserId(),
                cycle.getArchivedAt(), cycle.getArchivedByUserId(), cycle.getCreatedAt(), cycle.getUpdatedAt());
    }
    public static MemberCharge charge() { return charge(PaymentAcceptancePolicy.UNTIL_DUE_DATE, null); }
    public static MemberCharge charge(PaymentAcceptancePolicy policy, Integer grace) {
        return MemberCharge.reconstitute(2L, 10L, 3L, 4L, AMOUNT, AMOUNT, DUE, policy, grace,
                policy == PaymentAcceptancePolicy.AFTER_DUE_DATE ? null : policy == PaymentAcceptancePolicy.UNTIL_DUE_DATE ? DUE : DUE.plusDays(grace == null ? 0 : grace),
                MemberChargeStatus.PENDING, NOW, null, null, null);
    }
    public static MemberPayment payment() {
        return MemberPayment.reconstitute(1L, 2L, AMOUNT, PaymentMethod.PIX, MemberPaymentStatus.PENDING_VALIDATION,
                NOW.minusSeconds(60), "billing/payment-receipts/private.pdf", null, null, null, null, null,
                null, "note", NOW, null);
    }
    public static MemberRefund eligibility() {
        return MemberRefund.createEligibilityForCanceledCycle(2L, 1L, 4L, 10L, AMOUNT, 99L,
                NOW, NOW.plus(Duration.ofDays(30)), NOW);
    }
    public static MemberRefund request() {
        return MemberRefund.createMemberRequest(2L, 1L, 4L, 10L, AMOUNT, 10L, NOW);
    }
}
