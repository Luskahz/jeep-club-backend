package com.jeepclub.backend.tools.infra.persistence.mapper;

import com.jeepclub.backend.tools.core.domain.model.Tool;
import com.jeepclub.backend.tools.infra.persistence.entity.ToolEntity;
import org.springframework.stereotype.Component;

@Component
public class ToolMapper {

    public Tool toDomain(ToolEntity entity) {
        if (entity == null) return null;

        return Tool.reconstitute(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getUserId(),
                entity.getCreatedAt(), // <-- Datas adicionadas!
                entity.getUpdatedAt(), // <-- Datas adicionadas!
                entity.getDeletedAt()  // <-- Datas adicionadas!
        );
    }

    public ToolEntity toEntity(Tool domain) {
        if (domain == null) return null;

        ToolEntity entity = new ToolEntity();
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setStatus(domain.getStatus());
        entity.setUserId(domain.getUserId());
        entity.setCreatedAt(domain.getCreatedAt()); // <-- Datas adicionadas!
        entity.setUpdatedAt(domain.getUpdatedAt()); // <-- Datas adicionadas!
        entity.setDeletedAt(domain.getDeletedAt()); // <-- Datas adicionadas!

        return entity;
    }
}