package com.jeepclub.backend.toolmanager.api.dto;

import com.jeepclub.backend.toolmanager.domain.enums.ToolStatus;
import com.jeepclub.backend.toolmanager.domain.model.Tool;

public class ToolResponseDTO {
    private Long id;
    private String name;
    private String description;
    private ToolStatus status;

    public ToolResponseDTO(Tool tool) {
        this.id = tool.getId();
        this.name = tool.getName();
        this.description = tool.getDescription();
        this.status = tool.getStatus();
    }
    // Getters
}