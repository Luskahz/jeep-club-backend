package com.jeepclub.backend.storage.core.domain.model;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;

class ModerationDecisionTest {
    @Test void rejectsMissingActorAndRestorationWithoutRestorationReason() {
        var now = Instant.parse("2026-01-01T00:00:00Z");
        assertThatThrownBy(() -> new ModerationDecision(1, null, ModerationDecision.State.HIDDEN, ModerationDecision.Reason.PRIVACY, null, 0, now)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ModerationDecision(1, null, ModerationDecision.State.VISIBLE, ModerationDecision.Reason.PRIVACY, null, 1, now)).isInstanceOf(IllegalArgumentException.class);
        var decision = new ModerationDecision(1, 2L, ModerationDecision.State.BLOCKED, ModerationDecision.Reason.PRIVACY, "internal only", 1, now);
        assertThat(decision.publicView().reasonCode()).isEqualTo("CONTENT_UNAVAILABLE");
        assertThat(decision.publicView().toString()).doesNotContain("internal only", "PRIVACY");
    }
}
