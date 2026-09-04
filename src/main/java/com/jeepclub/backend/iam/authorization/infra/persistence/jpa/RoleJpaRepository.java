package com.jeepclub.backend.iam.authorization.infra.persistence.jpa;

import com.jeepclub.backend.iam.authorization.core.domain.enums.RoleKind;
import com.jeepclub.backend.iam.authorization.infra.persistence.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleJpaRepository extends JpaRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByName(String name);

    Optional<RoleEntity> findByKind(RoleKind kind);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}