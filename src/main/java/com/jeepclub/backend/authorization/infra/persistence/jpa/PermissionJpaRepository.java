package com.jeepclub.backend.authorization.infra.persistence.jpa;

import com.jeepclub.backend.authorization.core.domain.enums.PermissionCode;
import com.jeepclub.backend.authorization.infra.persistence.entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionJpaRepository extends JpaRepository<PermissionEntity, Long> {

    Optional<PermissionEntity> findByCode(PermissionCode code);
}