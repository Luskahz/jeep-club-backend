package com.jeepclub.backend.authentication.core.domain.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Session {

    private Long id;
    private Long userId;
    private String refreshToken;
    private SessionStatus status;
    private Instant createdAt;
    private Instant expiresAt;

    public static Session create(Long userId, String refreshToken, Instant expiresAt) {
        return new Session(null, userId, refreshToken, SessionStatus.ACTIVE, Instant.now(), expiresAt);
    }

    public static Session reconstitute(Long id, Long userId, String refreshToken,
                                       SessionStatus status, Instant createdAt, Instant expiresAt) {
        return new Session(id, userId, refreshToken, status, createdAt, expiresAt);
    }

    public boolean isValid(Instant now) {
        return status == SessionStatus.ACTIVE && now.isBefore(expiresAt);
    }

    public void renew(String newRefreshToken, Instant newExpiresAt) {
        this.refreshToken = newRefreshToken;
        this.expiresAt = newExpiresAt;
    }

    public void revoke() {
        this.status = SessionStatus.REVOKED;
    }
}
