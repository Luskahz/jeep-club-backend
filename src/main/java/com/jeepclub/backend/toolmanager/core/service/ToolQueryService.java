package com.jeepclub.backend.toolmanager.core.service;

import com.jeepclub.backend.toolmanager.domain.model.Tool;
import com.jeepclub.backend.toolmanager.domain.port.ToolRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ToolQueryService {

    private final ToolRepository toolRepository;

    public ToolQueryService(ToolRepository toolRepository) {
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