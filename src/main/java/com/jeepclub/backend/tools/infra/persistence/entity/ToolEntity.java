package com.jeepclub.backend.tools.infra.persistence.entity;

import com.jeepclub.backend.tools.core.domain.enums.ToolStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "toolmanager_tools")
public class ToolEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ToolStatus status;

    @Column(name = "user_id", nullable = false)
    private Long userId; // <- NOVA COLUNA

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ToolStatus getStatus() { return status; }
    public void setStatus(ToolStatus status) { this.status = status; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}