package com.jeepclub.backend.dependents.api.module;
import java.util.Optional;
/** Parent lock before creating logical references. Read-only presentation uses DependentsQuery. */
public interface DependentLifecycleLock {
    record Eligibility(Long id, Long userId, boolean active) {}
    Optional<Eligibility> byId(Long id);
}
