package com.jeepclub.backend.dependents.api.module;

import java.util.Collection;
import java.util.Set;

public interface DependentsQuery {

    boolean existsById(Long dependentId);

    boolean existsActiveById(Long dependentId);

    Set<Long> findActiveDependentIdsByIds(Collection<Long> dependentIds);

    boolean isActiveDependentOfUser(
            Long dependentId,
            Long userId
    );
}
