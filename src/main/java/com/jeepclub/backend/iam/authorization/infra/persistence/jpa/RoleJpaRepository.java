package com.jeepclub.backend.iam.authorization.infra.persistence.jpa;

import com.jeepclub.backend.iam.authorization.core.domain.enums.RoleKind;
import com.jeepclub.backend.iam.authorization.infra.persistence.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleJpaRepository extends JpaRepository<RoleEntity, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select role from RoleEntity role where role.id = :id")
    Optional<RoleEntity> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

    Optional<RoleEntity> findByName(String name);

    Optional<RoleEntity> findByKind(RoleKind kind);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<RoleEntity> findLockedByKind(RoleKind kind);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}