package com.jeepclub.backend.dependents.core.repository;

import com.jeepclub.backend.dependents.core.domain.model.Dependent;

import java.util.List;
import java.util.Optional;

public interface DependentRepository {

    Dependent save(Dependent dependent);
    Optional<Dependent> findById(Long id);
    List<Dependent> findAllByUserId(Long socioId);
    Optional<Dependent> findActiveById(Long id);
    List<Dependent> findAllActiveByUserId(Long socioId);
    boolean existsActiveByCpf(String cpf);
    boolean existsActiveByCpfAndIdNot(
            String cpf,
            Long id
    );
}