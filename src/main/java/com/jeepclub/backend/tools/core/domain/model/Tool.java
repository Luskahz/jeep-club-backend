package com.jeepclub.backend.tools.core.domain.model;

import com.jeepclub.backend.tools.core.domain.enums.ToolStatus;
import com.jeepclub.backend.tools.core.domain.exception.ToolAccessDeniedException;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Tool {

    private Long id;
    private String name;
    private String description;
    private ToolStatus status;
    private Long userId;

    // Novos campos de auditoria
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Tool(Long id, String name, String description, ToolStatus status, Long userId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = status;
        this.userId = userId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // CREATE: Nasce automaticamente como ACTIVE e ganha a data de criação
    public static Tool create(String name, String description, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return new Tool(null, name, description, ToolStatus.ACTIVE, userId, now, now);
    }

    // RECONSTITUTE: Usado pelo Mapper para reconstruir o objeto vindo do banco
    public static Tool reconstitute(Long id, String name, String description, ToolStatus status, Long userId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Tool(id, name, description, status, userId, createdAt, updatedAt);
    }

    public void updateDetails(String name, String description) {
        if (name != null && !name.isBlank()) this.name = name;
        if (description != null && !description.isBlank()) this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    // --- NOVOS MÉTODOS DE COMPORTAMENTO ---

    public void activate() {
        this.status = ToolStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.status = ToolStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void assertBelongsTo(Long currentUserId) {
        if (!this.userId.equals(currentUserId)) {
            throw new ToolAccessDeniedException("Acesso negado: Esta ferramenta não pertence a você.");
        }
    }
}
