package com.jeepclub.backend.billing.core.application.query;

import com.jeepclub.backend.billing.api.module.MembershipChargeResult;
import com.jeepclub.backend.billing.core.domain.enums.ChargeRecurrenceType;
import com.jeepclub.backend.billing.core.domain.enums.charge.MemberChargeStatus;
import com.jeepclub.backend.billing.core.domain.enums.cycle.ChargeCycleStatus;
import com.jeepclub.backend.billing.core.domain.enums.cycle.PaymentAcceptancePolicy;
import com.jeepclub.backend.billing.core.domain.model.ChargeCycle;
import com.jeepclub.backend.billing.core.domain.model.MemberCharge;
import com.jeepclub.backend.billing.core.repository.ChargeCycleRepository;
import com.jeepclub.backend.billing.core.repository.MemberChargeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MembershipChargeQueryServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 21);

    @Mock private ChargeCycleRepository cycleRepository;
    @Mock private MemberChargeRepository chargeRepository;
    private MembershipChargeQueryService service;

    @BeforeEach
    void setUp() {
        service = new MembershipChargeQueryService(cycleRepository, chargeRepository,
                Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC));
    }

    @Test
    void shouldReturnChargeNotFoundWhenCurrentRecurringCycleWasNotGenerated() {
        givenCycles(cycle(30L, LocalDate.of(2025, 12, 31), ChargeRecurrenceType.YEARLY));

        assertThat(service.evaluate(10L, 20L)).isEqualTo(MembershipChargeResult.CHARGE_NOT_FOUND);
    }

    @Test
    void shouldMapPaidPendingOverdueAndExpiredFromRelevantCycle() {
        assertResult(MemberChargeStatus.PAID, TODAY.minusDays(10), null, MembershipChargeResult.SATISFIED);
        assertResult(MemberChargeStatus.PENDING, TODAY.plusDays(1), null, MembershipChargeResult.WITHIN_PAYMENT_PERIOD);
        assertResult(MemberChargeStatus.PENDING, TODAY.minusDays(1), null, MembershipChargeResult.PAYMENT_REQUIRED);
        assertResult(MemberChargeStatus.PENDING, TODAY.minusDays(10), TODAY.minusDays(1), MembershipChargeResult.PAYMENT_REQUIRED);
    }

    @Test
    void shouldNotLetHistoricalOrFutureCycleHideCurrentOverdueObligation() {
        givenCycles(
                cycle(30L, LocalDate.of(2025, 12, 31), ChargeRecurrenceType.YEARLY),
                cycle(32L, TODAY.plusDays(10), ChargeRecurrenceType.YEARLY),
                cycle(31L, TODAY.minusDays(10), ChargeRecurrenceType.YEARLY)
        );
        when(chargeRepository.findByChargeCycleIdAndUserId(31L, 20L))
                .thenReturn(Optional.of(charge(31L, MemberChargeStatus.PENDING, TODAY.minusDays(10), null)));

        assertThat(service.evaluate(10L, 20L)).isEqualTo(MembershipChargeResult.PAYMENT_REQUIRED);
    }

    @Test
    void shouldReturnChargeNotFoundWhenRelevantCycleHasNoUserCharge() {
        givenCycles(cycle(31L, TODAY.plusDays(10), ChargeRecurrenceType.YEARLY));
        when(chargeRepository.findByChargeCycleIdAndUserId(31L, 20L)).thenReturn(Optional.empty());

        assertThat(service.evaluate(10L, 20L)).isEqualTo(MembershipChargeResult.CHARGE_NOT_FOUND);
    }

    @Test
    void shouldUseTheSameReferenceDateForCycleSelectionAndEffectiveStatus() {
        Clock clockCrossingMidnight = new Clock() {
            private final AtomicInteger instantCalls = new AtomicInteger();

            @Override
            public ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return TODAY.plusDays(instantCalls.getAndIncrement())
                        .atStartOfDay(ZoneOffset.UTC)
                        .toInstant();
            }
        };
        service = new MembershipChargeQueryService(cycleRepository, chargeRepository, clockCrossingMidnight);
        givenCycles(cycle(31L, TODAY, ChargeRecurrenceType.YEARLY));
        when(chargeRepository.findByChargeCycleIdAndUserId(31L, 20L))
                .thenReturn(Optional.of(charge(31L, MemberChargeStatus.PENDING, TODAY, null)));

        assertThat(service.evaluate(10L, 20L)).isEqualTo(MembershipChargeResult.WITHIN_PAYMENT_PERIOD);
    }

    private void assertResult(MemberChargeStatus status, LocalDate dueDate,
                              LocalDate paymentAllowedUntil, MembershipChargeResult expected) {
        givenCycles(cycle(31L, dueDate, ChargeRecurrenceType.YEARLY));
        when(chargeRepository.findByChargeCycleIdAndUserId(31L, 20L))
                .thenReturn(Optional.of(charge(31L, status, dueDate, paymentAllowedUntil)));
        assertThat(service.evaluate(10L, 20L)).isEqualTo(expected);
    }

    private void givenCycles(ChargeCycle... cycles) {
        when(cycleRepository.findByChargeDefinitionId(10L, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(cycles)));
    }

    private static ChargeCycle cycle(Long id, LocalDate dueDate, ChargeRecurrenceType recurrence) {
        ChargeCycle cycle = mock(ChargeCycle.class);
        lenient().when(cycle.getId()).thenReturn(id);
        lenient().when(cycle.getDueDate()).thenReturn(dueDate);
        lenient().when(cycle.getStatus()).thenReturn(ChargeCycleStatus.GENERATED);
        lenient().when(cycle.getChargeDefinitionRecurrenceTypeSnapshot()).thenReturn(recurrence);
        return cycle;
    }

    private static MemberCharge charge(Long cycleId, MemberChargeStatus status,
                                       LocalDate dueDate, LocalDate paymentAllowedUntil) {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        PaymentAcceptancePolicy policy = paymentAllowedUntil == null
                ? PaymentAcceptancePolicy.AFTER_DUE_DATE
                : paymentAllowedUntil.equals(dueDate)
                ? PaymentAcceptancePolicy.UNTIL_DUE_DATE
                : PaymentAcceptancePolicy.UNTIL_DAYS_AFTER_DUE_DATE;
        Integer graceDays = policy == PaymentAcceptancePolicy.UNTIL_DAYS_AFTER_DUE_DATE
                ? (int) (paymentAllowedUntil.toEpochDay() - dueDate.toEpochDay()) : null;
        return MemberCharge.reconstitute(
                1L, 20L, 10L, cycleId, BigDecimal.TEN, BigDecimal.TEN, dueDate,
                policy, graceDays, paymentAllowedUntil, status, createdAt, null,
                status == MemberChargeStatus.PAID ? createdAt.plusSeconds(1) : null,
                status == MemberChargeStatus.CANCELED ? createdAt.plusSeconds(1) : null
        );
    }
}
