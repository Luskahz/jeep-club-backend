package com.jeepclub.backend.tools.infra.persistence.entity;

import com.jeepclub.backend.tools.core.domain.enums.ToolStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "tools") // Ajuste o nome da tabela se for diferente no seu banco
@Getter
@Setter
public class ToolEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ToolStatus status;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // --- Campos de Auditoria (Datas) ---

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}