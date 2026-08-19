package com.jeepclub.backend.dependents.infra.persistence.jpa;

import com.jeepclub.backend.dependents.core.domain.enums.DependentStatus;
import com.jeepclub.backend.dependents.infra.persistence.entity.DependentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DependentJpaRepository
        extends JpaRepository<DependentEntity, Long> {

    List<DependentEntity> findAllByUserId(
            Long userId
    );

    Optional<DependentEntity> findByIdAndStatus(
            Long id,
            DependentStatus status
    );

    List<DependentEntity> findAllByUserIdAndStatus(
            Long userId,
            DependentStatus status
    );

    boolean existsByCpfAndStatus(
            String cpf,
            DependentStatus status
    );

    boolean existsByCpfAndIdNotAndStatus(
            String cpf,
            Long id,
            DependentStatus status
    );

    boolean existsByIdAndStatus(
            Long id,
            DependentStatus status
    );

    boolean existsByIdAndUserIdAndStatus(
            Long id,
            Long userId,
            DependentStatus status
    );
}