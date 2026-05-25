package com.jeepclub.backend.tools.core.application.service;

import com.jeepclub.backend.tools.api.dto.ToolCreateRequestDTO;
import com.jeepclub.backend.tools.api.dto.ToolUpdateRequestDTO;
import com.jeepclub.backend.tools.core.application.exception.ToolNotFoundException;
import com.jeepclub.backend.tools.core.domain.model.Tool;
import com.jeepclub.backend.tools.core.repository.ToolRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ToolService {

    private final ToolRepository toolRepository;

    // CONSTRUTOR MANUAL ADICIONADO AQUI:
    public ToolService(ToolRepository toolRepository) {
        this.toolRepository = toolRepository;
    }

    public Page<Tool> listUserTools(Long userId, Pageable pageable) {
        return toolRepository.findByUserId(userId, pageable);
    }

    public Tool getToolDetails(Long toolId, Long userId) {
        Tool tool = toolRepository.findById(toolId)
                // Usando a exceção de Aplicação corretamente!
                .orElseThrow(() -> new ToolNotFoundException("Ferramenta não encontrada no banco de dados."));

        // Usando a exceção de Domínio (dentro da entidade)
        tool.assertBelongsTo(userId);

        return tool;
    }

    @Transactional
    public Tool createTool(ToolCreateRequestDTO request, Long userId) {
        Tool tool = Tool.create(
                request.name(),
                request.description(),
                request.status(),
                userId
        );

        return toolRepository.save(tool);
    }

    @Transactional
    public Tool updateTool(Long id, ToolUpdateRequestDTO request, Long userId) {
        Tool tool = getToolDetails(id, userId);

        tool.updateDetails(request.name(), request.description());

        if (request.status() != null) {
            tool.changeStatus(request.status());
        }

        return toolRepository.save(tool);
    }

    @Transactional
    public void deleteTool(Long id, Long userId) {
        Tool tool = getToolDetails(id, userId);
        toolRepository.delete(tool);
    }
}