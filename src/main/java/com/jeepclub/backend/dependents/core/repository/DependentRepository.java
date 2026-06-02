package com.jeepclub.backend.dependents.core.repository;

import com.jeepclub.backend.dependents.core.domain.model.Dependent;

import java.util.List;
import java.util.Optional;

public interface DependentRepository {
    Dependent save(Dependent dependent);
    Optional<Dependent> findById(Long id);
    List<Dependent> findAllBySocioId(Long socioId);
    void deleteById(Long id);
    boolean existsByCpf(String cpf);
    boolean existsByCpfAndIdNot(String cpf, Long id);
}

