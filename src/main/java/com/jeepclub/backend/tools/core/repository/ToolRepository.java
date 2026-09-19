package com.jeepclub.backend.tools.core.repository;

import com.jeepclub.backend.tools.core.domain.enums.ToolStatus;
import com.jeepclub.backend.tools.core.domain.model.Tool;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ToolRepository {

    Page<Tool> findByUserId(Long userId, Pageable pageable);

    Page<Tool> findAll(String name, ToolStatus status, Pageable pageable);

    Optional<Tool> findById(Long id);

    Tool save(Tool tool);

    void delete(
            Tool tool,
            Long deletedByUserId,
            LocalDateTime deletedAt
    );
}
