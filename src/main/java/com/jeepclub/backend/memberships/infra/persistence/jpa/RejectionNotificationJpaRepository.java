package com.jeepclub.backend.memberships.infra.persistence.jpa;

import com.jeepclub.backend.memberships.infra.persistence.entity.RejectionNotificationEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.Pageable;
import java.time.Instant;
import java.util.*;

public interface RejectionNotificationJpaRepository extends JpaRepository<RejectionNotificationEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from RejectionNotificationEntity n where n.id = :id")
    Optional<RejectionNotificationEntity> lock(Long id);
    Optional<RejectionNotificationEntity> findByApplicationId(Long applicationId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from RejectionNotificationEntity n where n.applicationId = :applicationId")
    Optional<RejectionNotificationEntity> lockByApplicationId(Long applicationId);
    @Query("""
        select n.id from RejectionNotificationEntity n
        where (n.state = 'PENDING' and n.nextAttemptAt <= :now)
           or (n.state = 'PROCESSING' and n.leaseUntil <= :now)
        order by n.id
        """)
    List<Long> due(Instant now, Pageable page);
}
