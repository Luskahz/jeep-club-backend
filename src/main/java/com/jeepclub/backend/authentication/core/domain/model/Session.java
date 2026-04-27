package com.jeepclub.backend.authentication.core.domain.model;

import com.jeepclub.backend.authentication.core.domain.enums.SessionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.Duration;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Session {

    private Long id;
    private Long userId;
    private Instant createdAt;
    private Instant expiresAt;
    private Instant loggedOutAt;
    private SessionStatus status;

    private Session(
            Long userId,
            Duration ttl
    ){
        this.userId = userId;
        Instant now = Instant.now();
        this.createdAt = now;
        this.expiresAt = now.plus(ttl);
        this.status = SessionStatus.ACTIVE;
    }

    @Contract("_, _, _, _ -> new")
    public static Session create(
            @NotNull Long userId,
            @NotNull @Positive Duration ttl
    ) {
        return new Session(
                userId,
                ttl
        );
    }

    public static Session reconstitute(
            Long id,
            Long userId,
            Instant createdAt,
            Instant expiresAt,
            Instant loggedOutAt,
            SessionStatus status

    ) {
        if (createdAt == null) throw new IllegalArgumentException("createdAt is required");
        if (expiresAt == null) throw new IllegalArgumentException("expiresAt is required");
        if (status == null) throw new IllegalArgumentException("sessionStatus is required");

        if(!expiresAt.isAfter(createdAt)){
            throw new IllegalArgumentException("A expiração deve ser após a criação");
        }

        switch (status) {
            case ACTIVE -> {
                if (loggedOutAt != null) throw new IllegalStateException("Sessão ativa não deveria estar deslogada...");
            }
            case LOGGED_OUT -> {
                if (loggedOutAt == null) throw new IllegalStateException("O status da sessão deslogada deveria ter sido atualizado...");
                if (loggedOutAt.isBefore(createdAt)) throw new IllegalStateException("A sessão deveria ter sido deslogada após a criação");
            }
            case REVOKED -> {
                if (loggedOutAt != null) throw new IllegalStateException("O status Revogado não deve ser usado para registrar logout");
            }
        }

        Session session = new Session();
        session.id = id;
        session.userId = userId;
        session.createdAt = createdAt;
        session.expiresAt = expiresAt;
        session.loggedOutAt = loggedOutAt;
        session.status = status;
        return session;
    }
    public boolean isNotExpired(Instant now) {
        if (now == null) {
            throw new IllegalArgumentException("now is required");
        }

        return expiresAt.isAfter(now);
    }
    public boolean isActive(Instant now) {
        return status == SessionStatus.ACTIVE && isNotExpired(now);
    }
    public boolean isRevoked() {
        return status == SessionStatus.REVOKED;
    }
    public boolean isLoggedOut() {
        return status == SessionStatus.LOGGED_OUT;
    }
    public boolean isValid(Instant now) {
        return isNotExpired(now) && !isRevoked() && !isLoggedOut();
    }
    public void revoke() {
        if (status != SessionStatus.ACTIVE) {
            throw new IllegalStateException("Só é possivel revogar sessões ativas");
        }
        this.status = SessionStatus.REVOKED;
    }
    public void logout(Instant now) {
        if (this.status != SessionStatus.ACTIVE) {
            return;
        }
        this.status = SessionStatus.LOGGED_OUT;
        this.loggedOutAt = now;
    }
}
