package com.jeepclub.backend.platform.logging;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public interface SystemLogJpaRepository extends JpaRepository<SystemLogEntity, Long> {

    @Modifying
    @Transactional
    @Query("delete from SystemLogEntity log where log.occurredAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
