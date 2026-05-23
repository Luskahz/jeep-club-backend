package com.jeepclub.backend.tools.infra.persistence.entity;

import com.jeepclub.backend.tools.core.domain.enums.ToolStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "toolmanager_tools")
@Getter
@Setter
@NoArgsConstructor
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
   }