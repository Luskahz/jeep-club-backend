package com.jeepclub.backend.tools.core.domain.model;

import com.jeepclub.backend.tools.core.domain.enums.ToolStatus;

public class Tool {
    private Long id;
    private String name;
    private String description;
    private ToolStatus status;
    private Long userId; // <- NOVO CAMPO

    public Tool(Long id, String name, String description, ToolStatus status, Long userId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = status;
        this.userId = userId;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public ToolStatus getStatus() { return status; }
    public Long getUserId() { return userId; }
}