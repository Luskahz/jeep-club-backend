package com.jeepclub.backend.billing.infra.persistence.adapter;

import com.jeepclub.backend.billing.core.domain.enums.cycle.PaymentAcceptancePolicy;
import com.jeepclub.backend.billing.core.domain.model.MemberCharge;
import com.jeepclub.backend.billing.infra.persistence.mapper.MemberChargeMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({MemberChargeAdapter.class, MemberChargeMapper.class})
class MemberChargeMembershipQueryAdapterTest {

    @Autowired
    private MemberChargeAdapter adapter;

    @Test
    void shouldSelectChargeForCycleAndUser() {
        adapter.save(charge(10L, 20L, 30L, LocalDate.of(2025, 12, 31)));
        MemberCharge expected = adapter.save(charge(10L, 20L, 31L, LocalDate.of(2026, 12, 31)));
        adapter.save(charge(11L, 20L, 32L, LocalDate.of(2027, 12, 31)));
        adapter.save(charge(10L, 21L, 33L, LocalDate.of(2027, 12, 31)));

        assertThat(adapter.findByChargeCycleIdAndUserId(31L, 20L))
                .hasValueSatisfying(found -> assertThat(found.getId()).isEqualTo(expected.getId()));
    }

    private static MemberCharge charge(
            Long definitionId,
            Long userId,
            Long cycleId,
            LocalDate dueDate
    ) {
        return MemberCharge.create(
                userId,
                definitionId,
                cycleId,
                BigDecimal.TEN,
                dueDate,
                PaymentAcceptancePolicy.AFTER_DUE_DATE,
                null,
                Instant.parse("2026-01-01T00:00:00Z")
        );
    }
}
