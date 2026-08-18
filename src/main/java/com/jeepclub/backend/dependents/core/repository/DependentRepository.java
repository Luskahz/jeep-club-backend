package com.jeepclub.backend.dependents.core.repository;

import com.jeepclub.backend.dependents.core.domain.model.Dependent;

import java.util.List;
import java.util.Optional;

public interface DependentRepository {

    Dependent save(Dependent dependent);

    // Admin / auditoria
    Optional<Dependent> findById(Long id);

    List<Dependent> findAllBySocioId(Long socioId);

    // Fluxo normal
    Optional<Dependent> findActiveById(Long id);

    List<Dependent> findAllActiveBySocioId(Long socioId);

    // Regras de unicidade
    boolean existsActiveByCpf(String cpf);

    boolean existsActiveByCpfAndIdNot(
            String cpf,
            Long id
    );
}