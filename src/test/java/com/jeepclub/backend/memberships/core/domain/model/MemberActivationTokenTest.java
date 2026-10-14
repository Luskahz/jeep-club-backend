package com.jeepclub.backend.memberships.core.domain.model;

import com.jeepclub.backend.memberships.core.application.exception.MemberActivationTokenAlreadyUsedException;
import com.jeepclub.backend.memberships.core.application.exception.MemberActivationTokenExpiredException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberActivationTokenTest {

    private static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");
    private static final Duration TTL = Duration.ofHours(48);

    @Test
    @DisplayName("Should create token calculating expiresAt correctly")
    void shouldCreateTokenSuccessfully() {
        MemberActivationToken token = MemberActivationToken.create(
                100L,
                "hashed-secret-token",
                TTL,
                NOW
        );

        assertThat(token.getApplicationId()).isEqualTo(100L);
        assertThat(token.getTokenHash()).isEqualTo("hashed-secret-token");
        assertThat(token.getCreatedAt()).isEqualTo(NOW);
        assertThat(token.getExpiresAt()).isEqualTo(NOW.plus(TTL));
        assertThat(token.getUsedAt()).isNull();
        assertThat(token.isUsed()).isFalse();
        assertThat(token.isExpired(NOW)).isFalse();
        assertThat(token.isValid(NOW)).isTrue();
    }

    @Test
    @DisplayName("Should detect expired token correctly")
    void shouldDetectExpiredToken() {
        MemberActivationToken token = MemberActivationToken.create(
                100L,
                "hashed-secret-token",
                TTL,
                NOW
        );

        Instant beforeExpiration = NOW.plus(TTL).minusSeconds(1);
        Instant exactExpiration = NOW.plus(TTL);
        Instant afterExpiration = NOW.plus(TTL).plusSeconds(1);

        assertThat(token.isExpired(beforeExpiration)).isFalse();
        assertThat(token.isExpired(exactExpiration)).isFalse();
        assertThat(token.isExpired(afterExpiration)).isTrue();
        assertThat(token.isValid(afterExpiration)).isFalse();
    }

    @Test
    @DisplayName("Should mark token as used and reject duplicate consumption")
    void shouldMarkAsUsedAndRejectDuplicate() {
        MemberActivationToken token = MemberActivationToken.create(
                100L,
                "hashed-secret-token",
                TTL,
                NOW
        );

        Instant usedAt = NOW.plusSeconds(120);
        token.markAsUsed(usedAt);

        assertThat(token.isUsed()).isTrue();
        assertThat(token.getUsedAt()).isEqualTo(usedAt);
        assertThat(token.isValid(NOW)).isFalse();

        assertThatThrownBy(() -> token.markAsUsed(usedAt.plusSeconds(10)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Token já foi utilizado.");
    }

    @Test
    @DisplayName("Should validate valid token without exception")
    void shouldValidateOrThrowForValidToken() {
        MemberActivationToken token = MemberActivationToken.create(
                100L,
                "hashed-secret-token",
                TTL,
                NOW
        );

        token.validateOrThrow(NOW.plusSeconds(300));
    }

    @Test
    @DisplayName("Should throw MemberActivationTokenAlreadyUsedException when token is used")
    void shouldThrowWhenTokenIsUsed() {
        MemberActivationToken token = MemberActivationToken.create(
                100L,
                "hashed-secret-token",
                TTL,
                NOW
        );
        token.markAsUsed(NOW.plusSeconds(300));

        assertThatThrownBy(() -> token.validateOrThrow(NOW.plusSeconds(600)))
                .isInstanceOf(MemberActivationTokenAlreadyUsedException.class);
    }

    @Test
    @DisplayName("Should throw MemberActivationTokenExpiredException when token is expired and not used")
    void shouldThrowWhenTokenIsExpired() {
        MemberActivationToken token = MemberActivationToken.create(
                100L,
                "hashed-secret-token",
                TTL,
                NOW
        );

        assertThatThrownBy(() -> token.validateOrThrow(NOW.plus(TTL).plusSeconds(1)))
                .isInstanceOf(MemberActivationTokenExpiredException.class);
    }

    @Test
    @DisplayName("Should reconstitute token correctly")
    void shouldReconstituteToken() {
        MemberActivationToken token = MemberActivationToken.reconstitute(
                1L, 100L, "hash", NOW.plus(TTL), NOW.plusSeconds(1800), NOW
        );

        assertThat(token.getId()).isEqualTo(1L);
        assertThat(token.getApplicationId()).isEqualTo(100L);
        assertThat(token.getTokenHash()).isEqualTo("hash");
        assertThat(token.getExpiresAt()).isEqualTo(NOW.plus(TTL));
        assertThat(token.getUsedAt()).isEqualTo(NOW.plusSeconds(1800));
        assertThat(token.getCreatedAt()).isEqualTo(NOW);
    }
}
