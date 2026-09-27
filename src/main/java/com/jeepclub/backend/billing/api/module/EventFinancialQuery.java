package com.jeepclub.backend.billing.api.module;

import java.time.Instant;
import java.util.List;

public interface EventFinancialQuery {
    List<State> findByEvent(Long eventId);
    default State evaluate(Long eventId, Long chargeDefinitionId, Long userId) {
        return findByEvent(eventId).stream().filter(s -> s.chargeDefinitionId().equals(chargeDefinitionId) && s.userId().equals(userId))
            .findFirst().orElse(new State(chargeDefinitionId, userId, null, "CHARGE_NOT_FOUND", null, null));
    }
    record State(Long chargeDefinitionId, Long userId, Long memberChargeId, String effectiveStatus,
                 String paymentStatus, Instant paymentSubmittedAt) {}
}
