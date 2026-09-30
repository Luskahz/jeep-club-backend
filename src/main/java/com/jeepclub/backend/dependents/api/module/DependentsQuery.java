package com.jeepclub.backend.dependents.api.module;

import java.util.Collection;
import java.util.Set;

public interface DependentsQuery {
    record Details(Long id, Long userId, String name, String cpf, String relationshipType, String status) {}
    java.util.List<Details> findDetailsByIds(Collection<Long> ids);
    /** Read-only presentation for completed Event reports; ownership checks remain active-only. */
    java.util.List<Details> findPresentationDetailsByIds(Collection<Long> ids);
    java.util.List<Details> findDetailsByUser(Long userId, long afterId, int limit);


    boolean existsById(Long dependentId);

    boolean existsActiveById(Long dependentId);

    Set<Long> findActiveDependentIdsByIds(Collection<Long> dependentIds);

    boolean isActiveDependentOfUser(
            Long dependentId,
            Long userId
    );
}
