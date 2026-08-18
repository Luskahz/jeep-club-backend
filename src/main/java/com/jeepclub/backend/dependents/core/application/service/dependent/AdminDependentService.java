package com.jeepclub.backend.dependents.core.application.service.dependent;

import com.jeepclub.backend.dependents.core.application.exception.DependentNotFoundException;
import com.jeepclub.backend.dependents.core.application.result.DependentResult;
import com.jeepclub.backend.dependents.core.domain.model.Dependent;
import com.jeepclub.backend.dependents.core.port.DependentMedicalProfilePort;
import com.jeepclub.backend.dependents.core.repository.DependentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDependentService {

    private final DependentRepository dependentRepository;
    private final DependentMedicalProfilePort medicalProfilePort;

    @Transactional(readOnly = true)
    public List<DependentResult> findAllBySocioId(Long socioId) {
        return dependentRepository.findAllBySocioId(socioId)
                .stream()
                .map(this::toResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public DependentResult findBySocioIdAndId(
            Long socioId,
            Long id
    ) {
        Dependent dependent = dependentRepository.findById(id)
                .orElseThrow(
                        () -> new DependentNotFoundException(id)
                );

        if (!dependent.getSocioId().equals(socioId)) {
            throw new DependentNotFoundException(id);
        }

        return toResult(dependent);
    }

    private DependentResult toResult(
            Dependent dependent
    ) {
        return new DependentResult(
                dependent,
                medicalProfilePort.findByDependentId(
                        dependent.getId()
                )
        );
    }
}