package com.jeepclub.backend.billing.api.module.spi;
/** Caller holds the definition lock; each provider checks only its own retained references with current reads. */
public interface ChargeDefinitionDeletionGuard {
    boolean hasRetainedReferences(Long definitionId);
}
