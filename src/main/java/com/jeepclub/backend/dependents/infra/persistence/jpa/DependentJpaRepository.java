package com.jeepclub.backend.dependents.infra.persistence.jpa;

import com.jeepclub.backend.dependents.core.domain.enums.DependentStatus;
import com.jeepclub.backend.dependents.infra.persistence.entity.DependentEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DependentJpaRepository
        extends JpaRepository<DependentEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select d
            from DependentEntity d
            where d.id = :id
            """)
    Optional<DependentEntity> findByIdForUpdate(
            @Param("id") Long id
    );

    Optional<DependentEntity> findByIdAndStatus(
            Long id,
            DependentStatus status
    );

    List<DependentEntity> findAllByUserId(
            Long userId
    );

    List<DependentEntity> findAllByUserIdAndStatus(
            Long userId,
            DependentStatus status
    );

    boolean existsByCpf(
            String cpf
    );

    boolean existsByCpfAndIdNot(
            String cpf,
            Long id
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
