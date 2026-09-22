package com.jeepclub.backend.memberships.core.application.query;

import com.jeepclub.backend.billing.api.module.MembershipChargeQuery;
import com.jeepclub.backend.billing.api.module.MembershipChargeResult;
import com.jeepclub.backend.memberships.api.module.MembershipAccessResult;
import com.jeepclub.backend.memberships.core.domain.model.MembershipBillingConfiguration;
import com.jeepclub.backend.memberships.core.repository.MembershipBillingConfigurationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MembershipAccessQueryServiceTest {

    @Mock
    private MembershipBillingConfigurationRepository configurationRepository;
    @Mock
    private MembershipChargeQuery chargeQuery;

    private MembershipAccessQueryService service;

    @BeforeEach
    void setUp() {
        service = new MembershipAccessQueryService(configurationRepository, chargeQuery);
    }

    @Test
    void shouldAllowWhenConfigurationIsMissing() {
        when(configurationRepository.findCurrent()).thenReturn(Optional.empty());

        assertThat(service.evaluate(20L)).isEqualTo(MembershipAccessResult.ALLOWED);
        verifyNoInteractions(chargeQuery);
    }

    @Test
    void shouldAllowWhenEnforcementIsDisabled() {
        when(configurationRepository.findCurrent()).thenReturn(Optional.of(configuration(false)));

        assertThat(service.evaluate(20L)).isEqualTo(MembershipAccessResult.ALLOWED);
        verifyNoInteractions(chargeQuery);
    }

    @Test
    void shouldAllowPendingPaidAndCanceledCharges() {
        assertFinancialResult(MembershipChargeResult.WITHIN_PAYMENT_PERIOD, MembershipAccessResult.ALLOWED);
        assertFinancialResult(MembershipChargeResult.SATISFIED, MembershipAccessResult.ALLOWED);
        assertFinancialResult(MembershipChargeResult.CANCELED, MembershipAccessResult.ALLOWED);
    }

    @Test
    void shouldRequirePaymentForBlockingFinancialStatus() {
        assertFinancialResult(MembershipChargeResult.PAYMENT_REQUIRED, MembershipAccessResult.PAYMENT_REQUIRED);
    }

    @Test
    void shouldDenyOperationallyWhenExpectedChargeIsMissing() {
        assertFinancialResult(MembershipChargeResult.CHARGE_NOT_FOUND, MembershipAccessResult.CHARGE_NOT_FOUND);
    }

    private void assertFinancialResult(
            MembershipChargeResult financialResult,
            MembershipAccessResult accessResult
    ) {
        reset(configurationRepository, chargeQuery);
        when(configurationRepository.findCurrent()).thenReturn(Optional.of(configuration(true)));
        when(chargeQuery.evaluate(10L, 20L)).thenReturn(financialResult);

        assertThat(service.evaluate(20L)).isEqualTo(accessResult);
    }

    private static MembershipBillingConfiguration configuration(boolean enabled) {
        return MembershipBillingConfiguration.create(
                10L,
                enabled,
                Instant.parse("2026-09-21T12:00:00Z")
        );
    }
}
