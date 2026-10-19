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
    private final com.jeepclub.backend.platform.logging.SystemLogService audit;

    @Transactional
    public Tool updatePhoto(Long id, String storageKey) {
        Tool tool = getForUpdate(id);
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
        Tool tool = getForUpdate(id);
        tool.updateDetails(name, description, now());
        return toolRepository.save(tool);
    }

    @Transactional
    public Tool activateTool(Long id) {
        Tool tool = getForUpdate(id);
        return tool.activate(now()) ? toolRepository.save(tool) : tool;
    }

    @Transactional
    public Tool deactivateTool(Long id) {
        Tool tool = getForUpdate(id);
        return tool.deactivate(now()) ? toolRepository.save(tool) : tool;
    }

    @Transactional
    public void deleteTool(Long id, Long deletedByUserId) {
        Tool tool = getForUpdate(id);
        toolRepository.delete(tool, deletedByUserId, now());
        record(deletedByUserId,id,"DELETE","DELETE",204);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    @Transactional
    public Tool archiveTool(Long id, Long actorId) {
        var tool=getForUpdate(id); var now=now();
        if(tool.archive(now)) {
            tool=toolRepository.save(tool);
            audit.recordInTransaction(new com.jeepclub.backend.platform.logging.SystemLogEvent(actorId,
                    "TOOL_ARCHIVE","PATCH","/tools/"+id+"/archive",200,
                    com.jeepclub.backend.platform.logging.SystemLogOutcome.SUCCESS,0,null,java.time.Instant.now(clock)));
        }
        return tool;
    }

    @Transactional
    public Tool unarchiveTool(Long id, Long actorId) {
        var tool=getForUpdate(id); var now=now();
        if(tool.unarchive(now)) {
            tool=toolRepository.save(tool);
            audit.recordInTransaction(new com.jeepclub.backend.platform.logging.SystemLogEvent(actorId,
                    "TOOL_UNARCHIVE","PATCH","/tools/"+id+"/unarchive",200,
                    com.jeepclub.backend.platform.logging.SystemLogOutcome.SUCCESS,0,null,java.time.Instant.now(clock)));
        }
        return tool;
    }

    private Tool getForUpdate(Long id) {
        var tool=toolRepository.findByIdForUpdate(id).orElseThrow(() -> new ToolNotFoundException("Tool not found."));
        return tool;
    }
    @Transactional
    public Tool activateTool(Long id,Long actorId) {
        Tool tool=getForUpdate(id);
        if(!tool.activate(now())) return tool;
        Tool saved=toolRepository.save(tool);
        record(actorId,id,"ACTIVATE","PATCH",200);
        return saved;
    }
    @Transactional
    public Tool deactivateTool(Long id,Long actorId) {
        Tool tool=getForUpdate(id);
        if(!tool.deactivate(now())) return tool;
        Tool saved=toolRepository.save(tool);
        record(actorId,id,"DEACTIVATE","PATCH",200);
        return saved;
    }
    private void record(Long actorId,Long id,String action,String method,int status) {
        audit.recordInTransaction(new com.jeepclub.backend.platform.logging.SystemLogEvent(actorId,"TOOL_"+action,method,"/tools/"+id,status,
                com.jeepclub.backend.platform.logging.SystemLogOutcome.SUCCESS,0,null,java.time.Instant.now(clock)));
    }

}
