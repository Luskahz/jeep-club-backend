package com.jeepclub.backend.tools.core.repository;

import com.jeepclub.backend.tools.core.domain.model.Tool;
import java.util.List;
import java.util.Optional;

public interface ToolRepository {
    List<Tool> findAllByUserId(Long userId);
    Optional<Tool> findByIdAndUserId(Long toolId, Long userId);
}