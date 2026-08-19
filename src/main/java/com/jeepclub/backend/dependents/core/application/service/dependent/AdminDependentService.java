package com.jeepclub.backend.dependents.core.application.service.dependent;

import com.jeepclub.backend.dependents.core.application.exception.DependentNotFoundException;
import com.jeepclub.backend.dependents.core.application.result.DependentResult;
import com.jeepclub.backend.dependents.core.domain.model.Dependent;
import com.jeepclub.backend.dependents.core.repository.DependentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDependentService {

    private final DependentRepository dependentRepository;

    @Transactional(readOnly = true)
    public List<DependentResult> findAllByUserId(Long userId) {
        return dependentRepository.findAllByUserId(userId)
                .stream()
                .map(this::toResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public DependentResult findByUserIdAndId(
            Long userId,
            Long id
    ) {
        Dependent dependent = dependentRepository.findById(id)
                .orElseThrow(
                        () -> new DependentNotFoundException(id)
                );

        if (!dependent.getUserId().equals(userId)) {
            throw new DependentNotFoundException(id);
        }

        return toResult(dependent);
    }

    private DependentResult toResult(Dependent dependent) {
        return new DependentResult(dependent);
    }
}
