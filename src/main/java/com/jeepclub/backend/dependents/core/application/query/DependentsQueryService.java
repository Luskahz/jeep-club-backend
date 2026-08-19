package com.jeepclub.backend.dependents.core.application.query;

import com.jeepclub.backend.dependents.api.module.DependentsQuery;
import com.jeepclub.backend.dependents.core.repository.DependentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DependentsQueryService implements DependentsQuery {

    private final DependentRepository dependentRepository;

    @Override
    public boolean existsActiveById(Long dependentId) {
        if (dependentId == null) {
            return false;
        }

        return dependentRepository.existsActiveById(dependentId);
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