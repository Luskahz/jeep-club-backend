package com.jeepclub.backend.iam.identity.infra.persistence.jpa;

import com.jeepclub.backend.iam.identity.api.module.UserStatus;
import com.jeepclub.backend.iam.identity.infra.persistence.entity.UserEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT identity
            FROM UserEntity identity
            WHERE identity.id = :id
            """)
    Optional<UserEntity> findByIdForUpdate(@Param("id") Long id);

    Optional<UserEntity> findByCpf(String cpf);

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);

    boolean existsByRg(String rg);

    boolean existsByIdAndStatus(Long id, UserStatus status);

    @Query("""
            SELECT identity.id
            FROM UserEntity identity
            WHERE identity.status = :status
            ORDER BY identity.id
            """)
    List<Long> findIdsByStatus(@Param("status") UserStatus status);

    @Query("""
            SELECT identity.id
            FROM UserEntity identity
            WHERE identity.id IN :ids
              AND identity.status = :status
            """)
    Set<Long> findIdsByIdInAndStatus(
            @Param("ids") Collection<Long> ids,
            @Param("status") UserStatus status
    );
}
