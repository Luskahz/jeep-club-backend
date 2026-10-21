package com.jeepclub.backend.tools.core.application.service.tool;

import com.jeepclub.backend.tools.core.application.exception.ToolNotFoundException;
import com.jeepclub.backend.platform.storage.image.ImageMediaService;
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
public class ToolService {

    private final ToolRepository toolRepository;
    private final Clock clock;
    private final ImageMediaService images;
    private final com.jeepclub.backend.platform.logging.SystemLogService audit;

    @Transactional
    public Tool updatePhoto(Long id, Long userId, String storageKey) {
        Tool tool = getForUpdate(id, userId);
        images.requireExisting(storageKey);
        tool.updatePhoto(storageKey, now());
        return toolRepository.save(tool);
    }

    @Transactional(readOnly = true)
    public Page<Tool> listUserTools(Long userId, Pageable pageable) {
        return toolRepository.findByUserId(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Tool getToolDetails(Long toolId, Long userId) {
        Tool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new ToolNotFoundException(
                        "Ferramenta não encontrada no banco de dados."
                ));

        tool.assertBelongsTo(userId);
        return tool;
    }

    @Transactional
    public Tool createTool(String name, String description, Long userId) {
        Tool tool = Tool.create(name, description, userId, now());
        return toolRepository.save(tool);
    }

    @Transactional
    public Tool updateTool(
            Long id,
            String name,
            String description,
            Long userId
    ) {
        Tool tool = getForUpdate(id, userId);
        tool.updateDetails(name, description, now());
        return toolRepository.save(tool);
    }

    @Transactional
    public Tool activateTool(Long id, Long userId) {
        Tool tool = getForUpdate(id, userId);
        if(!tool.activate(now())) return tool;
        Tool saved=toolRepository.save(tool);
        record(userId,id,"ACTIVATE","PATCH",200);
        return saved;
    }

    @Transactional
    public Tool deactivateTool(Long id, Long userId) {
        Tool tool = getForUpdate(id, userId);
        if(!tool.deactivate(now())) return tool;
        Tool saved=toolRepository.save(tool);
        record(userId,id,"DEACTIVATE","PATCH",200);
        return saved;
    }

    @Transactional
    public void deleteTool(Long id, Long userId) {
        Tool tool = getForUpdate(id, userId);
        toolRepository.delete(
                tool,
                userId,
                now()
        );
        record(userId,id,"DELETE","DELETE",204);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    @Transactional
    public Tool archiveTool(Long id, Long userId) {
        var tool=getForUpdate(id,userId); var now=now();
        if(tool.archive(now)) {
            tool=toolRepository.save(tool);
            audit.recordInTransaction(new com.jeepclub.backend.platform.logging.SystemLogEvent(userId,
                    "TOOL_ARCHIVE","PATCH","/tools/"+id+"/archive",200,
                    com.jeepclub.backend.platform.logging.SystemLogOutcome.SUCCESS,0,null,java.time.Instant.now(clock)));
        }
        return tool;
    }

    @Transactional
    public Tool unarchiveTool(Long id, Long userId) {
        var tool=getForUpdate(id,userId); var now=now();
        if(tool.unarchive(now)) {
            tool=toolRepository.save(tool);
            audit.recordInTransaction(new com.jeepclub.backend.platform.logging.SystemLogEvent(userId,
                    "TOOL_UNARCHIVE","PATCH","/tools/"+id+"/unarchive",200,
                    com.jeepclub.backend.platform.logging.SystemLogOutcome.SUCCESS,0,null,java.time.Instant.now(clock)));
        }
        return tool;
    }

    private Tool getForUpdate(Long id, Long userId) {
        var tool=toolRepository.findByIdForUpdate(id).orElseThrow(() -> new ToolNotFoundException("Tool not found."));
        tool.assertBelongsTo(userId);
        return tool;
    }
    private void record(Long actorId,Long id,String action,String method,int status) {
        audit.recordInTransaction(new com.jeepclub.backend.platform.logging.SystemLogEvent(actorId,"TOOL_"+action,method,"/tools/"+id,status,
                com.jeepclub.backend.platform.logging.SystemLogOutcome.SUCCESS,0,null,java.time.Instant.now(clock)));
    }

}
