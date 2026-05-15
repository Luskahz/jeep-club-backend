package com.jeepclub.backend.authentication.core.domain.model;

import com.jeepclub.backend.authentication.core.domain.enums.PasswordResetStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PasswordResetRequest {

    private Long id;
    private Long userId;
    private String tokenHash;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant usedAt;
    private PasswordResetStatus status;

    private PasswordResetRequest(Long userId, String tokenHash, Instant createdAt, Instant expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.status = PasswordResetStatus.PENDING;
    }

    public static PasswordResetRequest create(Long userId, String tokenHash, Instant createdAt, Instant expiresAt) {
        if (userId == null) throw new IllegalArgumentException("UserId cannot be null");
        if (tokenHash == null || tokenHash.isBlank()) throw new IllegalArgumentException("TokenHash cannot be empty");
        if (createdAt == null) throw new IllegalArgumentException("CreatedAt cannot be null");
        if (expiresAt == null) throw new IllegalArgumentException("ExpiresAt cannot be null");
        if (expiresAt.isBefore(createdAt)) throw new IllegalArgumentException("ExpiresAt must be after CreatedAt");

        return new PasswordResetRequest(userId, tokenHash, createdAt, expiresAt);
    }

    public static PasswordResetRequest reconstitute(Long id, Long userId, String tokenHash, Instant expiresAt, Instant createdAt, Instant usedAt, PasswordResetStatus status) {
        PasswordResetRequest request = new PasswordResetRequest();
        request.id = id;
        request.userId = userId;
        request.tokenHash = tokenHash;
        request.expiresAt = expiresAt;
        request.createdAt = createdAt;
        request.usedAt = usedAt;
        request.status = status;
        return request;
    }

    public boolean isPending(Instant now) {
        if (status != PasswordResetStatus.PENDING) return false;
        if (now.isAfter(expiresAt)) return false;
        return true;
    }

    public void markAsUsed(Instant now) {
        if (!isPending(now)) {
            throw new IllegalStateException("Password reset request is no longer valid or has expired");
        }
        this.status = PasswordResetStatus.USED;
        this.usedAt = now;
    }

    public void cancel() {
        if (this.status == PasswordResetStatus.PENDING) {
            this.status = PasswordResetStatus.CANCELLED;
        }
    }
}
