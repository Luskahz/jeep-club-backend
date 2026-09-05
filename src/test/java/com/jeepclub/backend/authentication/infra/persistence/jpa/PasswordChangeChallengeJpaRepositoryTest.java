package com.jeepclub.backend.authentication.infra.persistence.jpa;

import com.jeepclub.backend.iam.authentication.infra.persistence.entity.PasswordChangeChallengeEntity;
import com.jeepclub.backend.iam.authentication.infra.persistence.entity.RefreshTokenEntity;
import com.jeepclub.backend.iam.authentication.infra.persistence.entity.SessionEntity;
import com.jeepclub.backend.iam.authentication.core.domain.enums.RefreshTokenStatus;
import com.jeepclub.backend.iam.authentication.core.domain.enums.SessionStatus;
import com.jeepclub.backend.iam.authentication.infra.persistence.jpa.PasswordChangeChallengeJpaRepository;
import com.jeepclub.backend.iam.authentication.infra.persistence.jpa.RefreshTokenJpaRepository;
import com.jeepclub.backend.iam.authentication.infra.persistence.jpa.SessionJpaRepository;
import jakarta.persistence.EntityManager;
import com.jeepclub.backend.iam.identity.infra.persistence.entity.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class PasswordChangeChallengeJpaRepositoryTest {

    @Autowired
    private PasswordChangeChallengeJpaRepository repository;

    @Autowired
    private SessionJpaRepository sessionRepository;

    @Autowired
    private RefreshTokenJpaRepository refreshTokenRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("invalidateActiveByUserId invalida apenas challenges ativos e nao expirados do usuario alvo")
    void shouldInvalidateOnlyActiveNonExpiredChallengesForTargetUser() {
        Instant now = Instant.parse("2026-05-21T18:00:00Z");

        PasswordChangeChallengeEntity activeTargetChallenge = challenge(
                10L,
                "token-active-target",
                now.minusSeconds(300),
                now.plusSeconds(3600),
                false,
                null
        );
        PasswordChangeChallengeEntity expiredTargetChallenge = challenge(
                10L,
                "token-expired-target",
                now.minusSeconds(7200),
                now.minusSeconds(60),
                false,
                null
        );
        PasswordChangeChallengeEntity usedTargetChallenge = challenge(
                10L,
                "token-used-target",
                now.minusSeconds(600),
                now.plusSeconds(3600),
                true,
                now.minusSeconds(120)
        );
        PasswordChangeChallengeEntity activeOtherUserChallenge = challenge(
                99L,
                "token-active-other-user",
                now.minusSeconds(300),
                now.plusSeconds(3600),
                false,
                null
        );

        repository.saveAll(List.of(
                activeTargetChallenge,
                expiredTargetChallenge,
                usedTargetChallenge,
                activeOtherUserChallenge
        ));
        repository.flush();

        int updatedRows = repository.invalidateActiveByUserId(10L, now);
        repository.flush();
        entityManager.clear();

        PasswordChangeChallengeEntity persistedActiveTarget = repository.findByTokenHash("token-active-target").orElseThrow();
        PasswordChangeChallengeEntity persistedExpiredTarget = repository.findByTokenHash("token-expired-target").orElseThrow();
        PasswordChangeChallengeEntity persistedUsedTarget = repository.findByTokenHash("token-used-target").orElseThrow();
        PasswordChangeChallengeEntity persistedActiveOtherUser = repository.findByTokenHash("token-active-other-user").orElseThrow();

        assertEquals(1, updatedRows);

        assertTrue(persistedActiveTarget.isUsed());
        assertEquals(now, persistedActiveTarget.getUsedAt());

        assertFalse(persistedExpiredTarget.isUsed());
        assertNull(persistedExpiredTarget.getUsedAt());

        assertTrue(persistedUsedTarget.isUsed());
        assertEquals(now.minusSeconds(120), persistedUsedTarget.getUsedAt());

        assertFalse(persistedActiveOtherUser.isUsed());
        assertNull(persistedActiveOtherUser.getUsedAt());
    }

    @Test
    @DisplayName("bulk revocation affects only active sessions and tokens of target user")
    void shouldRevokeActiveSessionsAndRefreshTokensForTargetUser() {
        Instant now = Instant.parse("2026-05-21T18:00:00Z");
        SessionEntity target = session(10L, now);
        SessionEntity other = session(99L, now);
        sessionRepository.saveAll(List.of(target, other));
        sessionRepository.flush();

        RefreshTokenEntity targetToken = token(target.getId(), "target-token", now);
        RefreshTokenEntity otherToken = token(other.getId(), "other-token", now);
        refreshTokenRepository.saveAll(List.of(targetToken, otherToken));
        refreshTokenRepository.flush();

        refreshTokenRepository.revokeActiveByUserId(10L);
        sessionRepository.revokeActiveByUserId(10L);
        entityManager.clear();

        assertEquals(SessionStatus.REVOKED,
                sessionRepository.findById(target.getId()).orElseThrow().getStatus());
        assertEquals(SessionStatus.ACTIVE,
                sessionRepository.findById(other.getId()).orElseThrow().getStatus());
        assertEquals(RefreshTokenStatus.REVOKED,
                refreshTokenRepository.findById(targetToken.getId()).orElseThrow().getStatus());
        assertEquals(RefreshTokenStatus.ACTIVE,
                refreshTokenRepository.findById(otherToken.getId()).orElseThrow().getStatus());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableJpaRepositories(basePackageClasses = PasswordChangeChallengeJpaRepository.class)
    @EntityScan(basePackageClasses = {PasswordChangeChallengeEntity.class, UserEntity.class})
    static class TestConfiguration {
    }

    private PasswordChangeChallengeEntity challenge(
            Long userId,
            String tokenHash,
            Instant createdAt,
            Instant expiresAt,
            boolean used,
            Instant usedAt
    ) {
        PasswordChangeChallengeEntity entity = new PasswordChangeChallengeEntity();
        entity.setUserId(userId);
        entity.setTokenHash(tokenHash);
        entity.setCreatedAt(createdAt);
        entity.setExpiresAt(expiresAt);
        entity.setUsed(used);
        entity.setUsedAt(usedAt);
        return entity;
    }

    private SessionEntity session(Long userId, Instant now) {
        SessionEntity entity = new SessionEntity();
        entity.setUserId(userId);
        entity.setCreatedAt(now);
        entity.setExpiresAt(now.plusSeconds(3600));
        entity.setStatus(SessionStatus.ACTIVE);
        return entity;
    }

    private RefreshTokenEntity token(Long sessionId, String hash, Instant now) {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setSessionId(sessionId);
        entity.setTokenHash(hash);
        entity.setCreatedAt(now);
        entity.setExpiresAt(now.plusSeconds(3600));
        entity.setStatus(RefreshTokenStatus.ACTIVE);
        return entity;
    }
}
