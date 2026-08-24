package com.jeepclub.backend.dependents.infra.persistence.mapper;

import com.jeepclub.backend.dependents.infra.persistence.entity.DependentEntity;
import com.jeepclub.backend.dependents.infra.persistence.entity.DependentHistoryEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class DependentHistoryMapper {

    public DependentHistoryEntity toHistoryEntity(
            DependentEntity source,
            Long deletedByUserId,
            Instant deletedAt
    ) {
        if (source == null) {
            return null;
        }

        DependentHistoryEntity history = new DependentHistoryEntity();

        history.setDependentId(source.getId());
        history.setName(source.getName());
        history.setCpf(source.getCpf());
        history.setBirthDate(source.getBirthDate());
        history.setRelationshipType(source.getRelationshipType());
        history.setPhoneNumber(source.getPhoneNumber());
        history.setUserId(source.getUserId());
        history.setStatus(source.getStatus());
        history.setDeletedByUserId(deletedByUserId);
        history.setCreatedAt(source.getCreatedAt());
        history.setUpdatedAt(source.getUpdatedAt());
        history.setDeletedAt(deletedAt);

        return history;
    }
}