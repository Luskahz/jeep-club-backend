package com.jeepclub.backend.dependents.api.module;

public interface DependentsQuery {

    boolean existsActiveById(Long dependentId);

    boolean isActiveDependentOfUser(
            Long dependentId,
            Long userId
    );
}
