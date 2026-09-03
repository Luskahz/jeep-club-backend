package com.jeepclub.backend.identity.infra.persistence.jpa;

import com.jeepclub.backend.identity.api.module.IdentityStatus;
import com.jeepclub.backend.identity.infra.persistence.entity.IdentityEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IdentityJpaRepository extends JpaRepository<IdentityEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT identity
            FROM IdentityEntity identity
            WHERE identity.id = :id
            """)
    Optional<IdentityEntity> findByIdForUpdate(@Param("id") Long id);

    Optional<IdentityEntity> findByCpf(String cpf);

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);

    boolean existsByRg(String rg);

    boolean existsByIdAndStatus(Long id, IdentityStatus status);

    @Query("""
            SELECT identity.id
            FROM IdentityEntity identity
            WHERE identity.status = :status
            ORDER BY identity.id
            """)
    List<Long> findIdsByStatus(@Param("status") IdentityStatus status);
}
