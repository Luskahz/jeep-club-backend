package com.jeepclub.backend.memberships.infra.persistence.jpa;

import com.jeepclub.backend.memberships.infra.persistence.entity.RejectionNotificationAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RejectionNotificationAttemptJpaRepository extends JpaRepository<RejectionNotificationAttemptEntity, Long> {
    List<RejectionNotificationAttemptEntity> findByNotificationIdOrderById(Long notificationId);
}
