package com.jeepclub.backend.publications.core.application.service.internal;

import com.jeepclub.backend.billing.api.module.EventFinancialQuery;
import com.jeepclub.backend.publications.core.domain.model.EventChargeRule;

/** Shared by registration confirmation and operational reporting. */
public final class EventParticipationRequirement {
    private EventParticipationRequirement() {}

    public static boolean satisfied(EventChargeRule rule, EventFinancialQuery.State state) {
        return state != null
            && ("PAID".equals(state.effectiveStatus()) || "PENDING_VALIDATION".equals(state.paymentStatus()))
            && state.paymentSubmittedAt() != null
            && !state.paymentSubmittedAt().isAfter(rule.participationCutoff());
    }
}
