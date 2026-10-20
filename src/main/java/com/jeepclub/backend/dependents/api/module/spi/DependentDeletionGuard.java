package com.jeepclub.backend.dependents.api.module.spi;
/** Caller holds the aggregate lock; provider checks only its own retained references with current reads. */
public interface DependentDeletionGuard { boolean hasRetainedReferences(Long id); }
