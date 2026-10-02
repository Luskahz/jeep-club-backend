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
                 String paymentStatus, Instant paymentSubmittedAt, String definitionName, java.math.BigDecimal amount,
                 Long chargeCycleId, java.time.LocalDate dueDate) {
        public State(Long definition,Long user,Long charge,String effective,String payment,Instant submitted) {
            this(definition,user,charge,effective,payment,submitted,null,null,null,null);
        }
    }
}
