package com.jeepclub.backend.iam.authorization.infra.persistence.jpa;

import com.jeepclub.backend.shared.authorization.PermissionCode;
import com.jeepclub.backend.iam.authorization.infra.persistence.entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionJpaRepository extends JpaRepository<PermissionEntity, Long> {

    Optional<PermissionEntity> findByCode(PermissionCode code);
}