package com.jeepclub.backend.tools.core.application.service;

import com.jeepclub.backend.tools.api.http.dto.ToolCreateRequestDTO;
import com.jeepclub.backend.tools.api.http.dto.ToolUpdateRequestDTO;
import com.jeepclub.backend.tools.core.application.exception.ToolNotFoundException;
import com.jeepclub.backend.tools.core.domain.model.Tool;
import com.jeepclub.backend.tools.core.repository.ToolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ToolService {

    private final ToolRepository toolRepository;

    public Page<Tool> listUserTools(Long userId, Pageable pageable) {
        return toolRepository.findByUserId(userId, pageable);
    }

    public Tool getToolDetails(Long toolId, Long userId) {
        Tool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new ToolNotFoundException("Ferramenta não encontrada no banco de dados."));

        tool.assertBelongsTo(userId);
        return tool;
    }

    @Transactional
    public Tool createTool(ToolCreateRequestDTO request, Long userId) {
        Tool tool = Tool.create(request.name(), request.description(), userId);
        return toolRepository.save(tool);
    }

    @Transactional
    public Tool updateTool(Long id, ToolUpdateRequestDTO request, Long userId) {
        Tool tool = getToolDetails(id, userId);
        tool.updateDetails(request.name(), request.description());
        return toolRepository.save(tool);
    }

    @Transactional
    public Tool activateTool(Long id, Long userId) {
        Tool tool = getToolDetails(id, userId);
        tool.activate();
        return toolRepository.save(tool);
    }

    @Transactional
    public Tool deactivateTool(Long id, Long userId) {
        Tool tool = getToolDetails(id, userId);
        tool.deactivate();
        return toolRepository.save(tool);
    }

    @Transactional
    public void deleteTool(Long id, Long userId) {
        Tool tool = getToolDetails(id, userId);
        tool.softDelete();
        toolRepository.save(tool);
    }
}