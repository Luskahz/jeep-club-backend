package com.jeepclub.backend.iam.authorization.infra.persistence.jpa;

import com.jeepclub.backend.iam.authorization.infra.persistence.entity.RolePermissionDeletionHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePermissionDeletionHistoryJpaRepository extends JpaRepository<RolePermissionDeletionHistoryEntity, Long> {
    java.util.List<RolePermissionDeletionHistoryEntity> findByRoleIdOrderByOriginalId(Long roleId);
}
