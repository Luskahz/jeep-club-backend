package com.jeepclub.backend.billing.core.domain.model;

import com.jeepclub.backend.billing.infra.persistence.mapper.ChargeCycleMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import static com.jeepclub.backend.billing.support.BillingFixtures.*;
import static org.assertj.core.api.Assertions.*;

class EventCycleCancellationTest {
    @ParameterizedTest @ValueSource(ints = {0, 1, 2})
    void eventReversalRoundTripsEveryClosingStateAndDoesNotRewriteAuditOnRetry(int stage) {
        var cycle = cycle();
        if (stage >= 1) cycle.finish(98L, NOW);
        if (stage == 2) cycle.archive(97L, NOW.plusSeconds(1));
        assertThat(cycle.cancelForEvent(99L, NOW.plusSeconds(2))).isTrue();
        assertThat(cycle.cancelForEvent(96L, NOW.plusSeconds(3))).isFalse();
        assertThat(cycle.getCanceledByUserId()).isEqualTo(99L);
        assertThat(cycle.getCanceledAt()).isEqualTo(NOW.plusSeconds(2));
        if (stage >= 1) assertThat(cycle.getFinishedByUserId()).isEqualTo(98L);
        var mapper = new ChargeCycleMapper();
        assertThat(mapper.toDomain(mapper.toEntity(cycle))).usingRecursiveComparison().isEqualTo(cycle);
    }
    @Test void eventReversalCannotInventAChronologyBeforeFinishing() {
        var cycle = cycle(); cycle.finish(98L, NOW.plusSeconds(1));
        assertThatThrownBy(() -> cycle.cancelForEvent(99L, NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThat(cycle.isFinished()).isTrue(); assertThat(cycle.getCanceledAt()).isNull();
    }
}
