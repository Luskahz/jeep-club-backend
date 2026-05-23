package com.jeepclub.backend.tools.api.dto;

import com.jeepclub.backend.tools.core.domain.enums.ToolStatus;
import com.jeepclub.backend.tools.core.domain.model.Tool;

public class ToolResponseDTO {
    private Long id;
    private String name;
    private String description;
    private ToolStatus status;
    private Long userId; // <- NOVO CAMPO

    public ToolResponseDTO(Tool tool) {
        this.id = tool.getId();
        this.name = tool.getName();
        this.description = tool.getDescription();
        this.status = tool.getStatus();
        this.userId = tool.getUserId();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public ToolStatus getStatus() { return status; }
    public Long getUserId() { return userId; }
}