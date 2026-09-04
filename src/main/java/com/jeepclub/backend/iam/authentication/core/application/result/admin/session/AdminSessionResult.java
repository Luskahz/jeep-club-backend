package com.jeepclub.backend.iam.authentication.core.application.result.admin.session;

import com.jeepclub.backend.iam.authentication.core.domain.model.Session;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record AdminSessionResult(
        Long id,
        Long userId,
        String status,
        Instant createdAt,
        Instant expiresAt,
        Instant loggedOutAt
) {

    public static AdminSessionResult from(Session session) {
        Objects.requireNonNull(session, "session cannot be null");

        return new AdminSessionResult(
                session.getId(),
                session.getUserId(),
                session.getStatus().name(),
                session.getCreatedAt(),
                session.getExpiresAt(),
                session.getLoggedOutAt()
        );
    }

    public static List<AdminSessionResult> from(List<Session> sessions) {
        Objects.requireNonNull(sessions, "sessions cannot be null");

        return sessions.stream()
                .map(AdminSessionResult::from)
                .toList();
    }
}