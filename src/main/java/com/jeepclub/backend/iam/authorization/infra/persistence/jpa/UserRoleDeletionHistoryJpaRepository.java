package com.jeepclub.backend.iam.authorization.infra.persistence.jpa;

import com.jeepclub.backend.iam.authorization.infra.persistence.entity.UserRoleDeletionHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleDeletionHistoryJpaRepository extends JpaRepository<UserRoleDeletionHistoryEntity, Long> {
    java.util.List<UserRoleDeletionHistoryEntity> findByRoleIdOrderByOriginalId(Long roleId);
}
