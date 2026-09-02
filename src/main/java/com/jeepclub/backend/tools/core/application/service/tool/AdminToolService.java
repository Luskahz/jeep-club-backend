package com.jeepclub.backend.tools.core.application.service.tool;

import com.jeepclub.backend.tools.core.application.exception.ToolNotFoundException;
import com.jeepclub.backend.tools.core.domain.enums.ToolStatus;
import com.jeepclub.backend.tools.core.domain.model.Tool;
import com.jeepclub.backend.tools.core.repository.ToolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminToolService {

    private final ToolRepository toolRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Page<Tool> listAllTools(String name, ToolStatus status, Pageable pageable) {
        return toolRepository.findAll(name, status, pageable);
    }

    @Transactional(readOnly = true)
    public Tool getToolDetails(Long toolId) {
        return toolRepository.findById(toolId)
                .orElseThrow(() -> new ToolNotFoundException("Ferramenta não encontrada no banco de dados."));
    }

    @Transactional
    public Tool createToolForUser(Long userId, String name, String description) {
        Tool tool = Tool.create(name, description, userId);
        return toolRepository.save(tool);
    }

    @Transactional
    public Tool updateTool(Long id, String name, String description) {
        Tool tool = getToolDetails(id);
        tool.updateDetails(name, description);
        return toolRepository.save(tool);
    }

    @Transactional
    public Tool activateTool(Long id) {
        Tool tool = getToolDetails(id);
        tool.activate();
        return toolRepository.save(tool);
    }

    @Transactional
    public Tool deactivateTool(Long id) {
        Tool tool = getToolDetails(id);
        tool.deactivate();
        return toolRepository.save(tool);
    }

    @Transactional
    public void deleteTool(Long id, Long deletedByUserId) {
        Tool tool = getToolDetails(id);
        toolRepository.delete(tool, deletedByUserId, LocalDateTime.now(clock));
    }
}