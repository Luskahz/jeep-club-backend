package com.jeepclub.backend.dependents.core.application.query;

import com.jeepclub.backend.dependents.api.module.DependentsQuery;
import com.jeepclub.backend.dependents.core.repository.DependentRepository;
import com.jeepclub.backend.dependents.core.repository.DependentHistoricalPresentationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DependentsQueryService implements DependentsQuery {
    private final DependentHistoricalPresentationRepository history;
    public java.util.List<Details> findDetailsByIds(Collection<Long> ids) {
        if(ids==null || ids.isEmpty())return java.util.List.of();
        if(ids.size()>500)throw new IllegalArgumentException("Maximum batch: 500");
        return dependentRepository.findByIds(ids).stream().map(DependentsQueryService::details).toList();
    }
    public java.util.List<Details> findPresentationDetailsByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return java.util.List.of();
        if (ids.size() > 500) throw new IllegalArgumentException("Maximum batch: 500");
        var current = dependentRepository.findByIds(ids).stream().map(DependentsQueryService::details).toList();
        var missing = new java.util.HashSet<>(ids);
        current.forEach(d -> missing.remove(d.id()));
        if (missing.isEmpty()) return current;
        var result = new java.util.ArrayList<>(current);
        result.addAll(history.findByOriginalIds(missing));
        return java.util.List.copyOf(result);
    }
    public java.util.List<Details> findDetailsByUser(Long userId,long afterId,int limit) {
        if(limit<1 || limit>500 || afterId<0)throw new IllegalArgumentException("Invalid batch");
        return dependentRepository.findByUserAfterId(userId,afterId,limit).stream().map(DependentsQueryService::details).toList();
    }
    private static Details details(com.jeepclub.backend.dependents.core.domain.model.Dependent d) {
        return new Details(d.getId(),d.getUserId(),d.getName(),d.getCpf(),d.getRelationshipType().name(),d.getStatus().name());
    }


    private final DependentRepository dependentRepository;

    @Override
    public boolean existsById(Long dependentId) {
        return dependentId != null
                && dependentRepository.findById(dependentId).isPresent();
    }

    @Override
    public boolean existsActiveById(Long dependentId) {
        if (dependentId == null) {
            return false;
        }

        return dependentRepository.existsActiveById(dependentId);
    }

    @Override
    public Set<Long> findActiveDependentIdsByIds(
            Collection<Long> dependentIds
    ) {
        return dependentIds == null || dependentIds.isEmpty()
                ? Set.of()
                : dependentRepository.findActiveIdsByIds(dependentIds);
    }

    @Override
    public boolean isActiveDependentOfUser(
            Long dependentId,
            Long userId
    ) {
        if (dependentId == null || userId == null) {
            return false;
        }

        return dependentRepository.existsActiveByIdAndUserId(
                dependentId,
                userId
        );
    }
}
