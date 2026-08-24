package com.jeepclub.backend.dependents.core.repository;

import com.jeepclub.backend.dependents.core.domain.model.Dependent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DependentRepository {

    Dependent save(Dependent dependent);

    Optional<Dependent> findById(Long id);

    Optional<Dependent> findActiveById(Long id);

    List<Dependent> findAllByUserId(Long userId);

    List<Dependent> findAllActiveByUserId(Long userId);

    boolean existsByCpf(String cpf);

    boolean existsByCpfAndIdNot(
            String cpf,
            Long id
    );

    boolean existsActiveById(Long id);

    boolean existsActiveByIdAndUserId(
            Long id,
            Long userId
    );

    void delete(
            Dependent dependent,
            Long deletedByUserId,
            Instant deletedAt
    );
}