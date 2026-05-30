package com.jeepclub.backend.tools.infra.persistence.jpa;

import com.jeepclub.backend.tools.core.domain.enums.ToolStatus;
import com.jeepclub.backend.tools.infra.persistence.entity.ToolEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ToolJpaRepository extends JpaRepository<ToolEntity, Long> {

    // Traz a lista paginada ignorando as que têm status DELETED
    Page<ToolEntity> findByUserIdAndStatusNot(Long userId, ToolStatus status, Pageable pageable);

    Optional<ToolEntity> findByIdAndUserId(Long id, Long userId);
}