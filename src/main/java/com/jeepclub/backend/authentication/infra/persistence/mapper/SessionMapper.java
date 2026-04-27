package com.jeepclub.backend.authentication.infra.persistence.mapper;

import com.jeepclub.backend.authentication.core.domain.model.Session;
import com.jeepclub.backend.authentication.infra.persistence.entities.SessionEntity;

public class SessionMapper {

    private SessionMapper() {}

    public static Session toDomain(SessionEntity entity) {
        if (entity == null) return null;
        return Session.reconstitute(
                entity.getId(),
                entity.getUserId(),
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                entity.getLoggedOutAt(),
                entity.getStatus()
        );
    }

    public static SessionEntity toEntity(Session domain) {
        if (domain == null) return null;
        SessionEntity entity = new SessionEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setExpiresAt(domain.getExpiresAt());
        entity.setLoggedOutAt(domain.getLoggedOutAt());
        entity.setStatus(domain.getStatus());
        return entity;
    }
}