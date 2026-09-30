package com.jeepclub.backend.dependents.infra.persistence.query;

import com.jeepclub.backend.dependents.api.module.DependentsQuery;
import com.jeepclub.backend.dependents.core.repository.DependentHistoricalPresentationRepository;
import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository @RequiredArgsConstructor
public class DependentHistoricalPresentationJpaQuery implements DependentHistoricalPresentationRepository {
    private final EntityManager em;

    public List<DependentsQuery.Details> findByOriginalIds(Collection<Long> ids) {
        if (ids.isEmpty()) return List.of();
        return em.createQuery("select h.dependentId,h.userId,h.name,h.cpf,h.relationshipType,h.status from DependentHistoryEntity h where h.dependentId in :ids", Object[].class)
            .setParameter("ids", ids).getResultList().stream()
            .map(row -> new DependentsQuery.Details((Long) row[0], (Long) row[1], (String) row[2],
                (String) row[3], row[4].toString(), row[5].toString())).toList();
    }
}
