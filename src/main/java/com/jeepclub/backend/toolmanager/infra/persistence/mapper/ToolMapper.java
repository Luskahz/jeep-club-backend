package com.jeepclub.backend.toolmanager.infra.persistence.mapper;

import com.jeepclub.backend.toolmanager.domain.model.Tool;
import com.jeepclub.backend.toolmanager.infra.persistence.entity.ToolEntity;
import org.springframework.stereotype.Component;

@Component
public class ToolMapper {
    public Tool toDomain(ToolEntity entity) {
        return new Tool(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getUserId() // <- PASSANDO O USER ID AQUI
        );
    }
}