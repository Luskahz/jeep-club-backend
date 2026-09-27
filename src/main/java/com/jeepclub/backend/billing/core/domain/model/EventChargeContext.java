package com.jeepclub.backend.billing.core.domain.model;

/** Structured, retained identity of the assignment that originated a financial cycle. */
public record EventChargeContext(Long eventId, Long chargeDefinitionId, Long assignmentId, Long cycleId) {
    public EventChargeContext {
        for (Long id : java.util.Arrays.asList(eventId, chargeDefinitionId, assignmentId, cycleId))
            if (id == null || id <= 0) throw new IllegalArgumentException("Context identities must be positive.");
    }
}
