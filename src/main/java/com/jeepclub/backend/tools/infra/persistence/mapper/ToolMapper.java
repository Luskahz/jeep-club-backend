package com.jeepclub.backend.tools.infra.persistence.mapper;

import com.jeepclub.backend.tools.core.domain.model.Tool;
import com.jeepclub.backend.tools.infra.persistence.entity.ToolEntity;
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