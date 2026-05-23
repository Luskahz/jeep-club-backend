package com.jeepclub.backend.tools.core.application.service;

import com.jeepclub.backend.tools.core.domain.model.Tool;
import com.jeepclub.backend.tools.core.repository.ToolRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ToolService {

    private final ToolRepository toolRepository;

    public ToolService(ToolRepository toolRepository) {
        this.toolRepository = toolRepository;
    }

    // Atualizado: Agora recebe o ID do usuário
    public List<Tool> listUserTools(Long userId) {
        return toolRepository.findAllByUserId(userId);
    }

    // Atualizado: Agora confere o ID da ferramenta e se ela pertence ao usuário
    public Tool getToolDetails(Long toolId, Long userId) {
        return toolRepository.findByIdAndUserId(toolId, userId)
                .orElseThrow(() -> new RuntimeException("Tool not found or does not belong to you"));
    }
}