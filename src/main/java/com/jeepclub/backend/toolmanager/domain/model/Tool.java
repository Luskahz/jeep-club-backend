package com.jeepclub.backend.toolmanager.domain.model;

import com.jeepclub.backend.toolmanager.domain.enums.ToolStatus;

public class Tool {
    private Long id;
    private String name;
    private String description;
    private ToolStatus status;

    // Construtores, Getters e Setters (ou use @Data/@Builder do Lombok se o projeto usar)
    public Tool(Long id, String name, String description, ToolStatus status) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = status;
    }
    // ... adicione os getters e setters
}