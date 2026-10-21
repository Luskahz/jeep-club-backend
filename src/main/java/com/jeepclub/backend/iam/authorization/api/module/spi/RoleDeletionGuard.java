package com.jeepclub.backend.iam.authorization.api.module.spi;

/** Caller holds the Role lock. Providers read their own retained references with current reads. */
public interface RoleDeletionGuard {
    boolean hasRetainedReferences(Long roleId);
}
