package com.jeepclub.backend.tools.core.repository;

import com.jeepclub.backend.tools.core.domain.model.Tool;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface ToolRepository {

    // Lista paginada
    Page<Tool> findByUserId(Long userId, Pageable pageable);

    // Busca um
    Optional<Tool> findById(Long id);

    // Valida o dono
    Optional<Tool> findByIdAndUserId(Long id, Long userId);

    // Métodos essenciais que o Service usa para o CRUD
    Tool save(Tool tool);
    void delete(Tool tool);
}