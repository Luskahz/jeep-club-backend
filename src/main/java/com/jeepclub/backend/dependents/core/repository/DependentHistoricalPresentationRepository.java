package com.jeepclub.backend.dependents.core.repository;

import com.jeepclub.backend.dependents.api.module.DependentsQuery;
import java.util.Collection;
import java.util.List;

public interface DependentHistoricalPresentationRepository {
    List<DependentsQuery.Details> findByOriginalIds(Collection<Long> ids);
}
