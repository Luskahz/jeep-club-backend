package com.jeepclub.backend.tools.infra.persistence.jpa;

import com.jeepclub.backend.tools.infra.persistence.entity.ToolHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ToolHistoryJpaRepository
        extends JpaRepository<ToolHistoryEntity, Long> {
}
