package com.jeepclub.backend.toolmanager.domain.port;

import com.jeepclub.backend.toolmanager.domain.model.Tool;
import java.util.List;
import java.util.Optional;

public interface ToolRepository {
    List<Tool> findAllAvailable();
    Optional<Tool> findById(Long id);
}