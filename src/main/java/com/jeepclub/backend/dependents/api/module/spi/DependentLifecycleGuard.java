package com.jeepclub.backend.dependents.api.module.spi;
import java.time.Instant;

/** Called under the Dependent lifecycle mutex; must never acquire Event locks. */
public interface DependentLifecycleGuard {
    boolean hasOperationalParticipation(Long dependentId, Instant now);
}
