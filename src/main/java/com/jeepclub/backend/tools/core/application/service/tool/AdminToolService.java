package com.jeepclub.backend.tools.core.application.service.tool;

import com.jeepclub.backend.tools.core.application.exception.ToolNotFoundException;
import com.jeepclub.backend.tools.core.application.exception.ToolOwnerNotFoundException;
import com.jeepclub.backend.tools.core.application.exception.ToolOwnerDisabledException;
import com.jeepclub.backend.tools.core.port.ToolOwnerQuery;
import com.jeepclub.backend.platform.storage.image.ImageMediaService;
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
    private final ToolOwnerQuery toolOwnerQuery;
    private final ImageMediaService images;

    @Transactional
    public Tool updatePhoto(Long id, String storageKey) {
        Tool tool = getToolDetails(id);
        images.requireExisting(storageKey);
        tool.updatePhoto(storageKey, now());
        return toolRepository.save(tool);
    }

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
        if (!toolOwnerQuery.existsById(userId)) {
            throw new ToolOwnerNotFoundException();
        }
        if (!toolOwnerQuery.isAdministrativelyActive(userId)) {
            throw new ToolOwnerDisabledException();
        }
        Tool tool = Tool.create(name, description, userId, now());
        return toolRepository.save(tool);
    }

    @Transactional
    public Tool updateTool(Long id, String name, String description) {
        Tool tool = getToolDetails(id);
        tool.updateDetails(name, description, now());
        return toolRepository.save(tool);
    }

    @Transactional
    public Tool activateTool(Long id) {
        Tool tool = getToolDetails(id);
        return tool.activate(now()) ? toolRepository.save(tool) : tool;
    }

    @Transactional
    public Tool deactivateTool(Long id) {
        Tool tool = getToolDetails(id);
        return tool.deactivate(now()) ? toolRepository.save(tool) : tool;
    }

    @Transactional
    public void deleteTool(Long id, Long deletedByUserId) {
        Tool tool = getToolDetails(id);
        toolRepository.delete(tool, deletedByUserId, now());
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
