package com.jeepclub.backend.iam.authorization.infra.persistence.jpa;

import com.jeepclub.backend.iam.authorization.infra.persistence.entity.RoleDeletionHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleDeletionHistoryJpaRepository extends JpaRepository<RoleDeletionHistoryEntity, Long> {
}
