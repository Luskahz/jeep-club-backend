package com.jeepclub.backend.toolmanager.infra.persistence.jpa;

import com.jeepclub.backend.toolmanager.domain.enums.ToolStatus;
import com.jeepclub.backend.toolmanager.infra.persistence.entity.ToolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ToolJpaRepository extends JpaRepository<ToolEntity, Long> {
    List<ToolEntity> findAllByStatus(ToolStatus status);
}