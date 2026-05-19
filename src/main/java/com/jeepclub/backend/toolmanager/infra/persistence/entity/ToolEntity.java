package com.jeepclub.backend.toolmanager.infra.persistence.entity;

import com.jeepclub.backend.toolmanager.domain.enums.ToolStatus;
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

    // Getters e Setters
}