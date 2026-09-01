package com.jeepclub.backend.tools.infra.persistence.mapper;

import com.jeepclub.backend.tools.infra.persistence.entity.ToolEntity;
import com.jeepclub.backend.tools.infra.persistence.entity.ToolHistoryEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ToolHistoryMapper {

    public ToolHistoryEntity toHistoryEntity(
            ToolEntity source,
            Long deletedByUserId,
            LocalDateTime deletedAt
    ) {
        if (source == null) {
            return null;
        }

        ToolHistoryEntity history = new ToolHistoryEntity();
        history.setToolId(source.getId());
        history.setName(source.getName());
        history.setDescription(source.getDescription());
        history.setStatus(source.getStatus());
        history.setUserId(source.getUserId());
        history.setDeletedByUserId(deletedByUserId);
        history.setCreatedAt(source.getCreatedAt());
        history.setUpdatedAt(source.getUpdatedAt());
        history.setDeletedAt(deletedAt);

        return history;
    }
}
