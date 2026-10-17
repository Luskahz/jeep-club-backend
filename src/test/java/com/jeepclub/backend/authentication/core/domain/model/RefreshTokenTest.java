package com.jeepclub.backend.authentication.core.domain.model;

import com.jeepclub.backend.iam.authentication.core.domain.enums.RefreshTokenStatus;
import com.jeepclub.backend.iam.authentication.core.domain.enums.SessionStatus;
import com.jeepclub.backend.iam.authentication.core.domain.exception.refreshtoken.RefreshTokenStateException;
import com.jeepclub.backend.iam.authentication.core.domain.exception.refreshtoken.RefreshTokenValidationException;
import com.jeepclub.backend.iam.authentication.core.domain.model.RefreshToken;
import com.jeepclub.backend.iam.authentication.core.domain.model.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshTokenTest {

    private static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");
    private static final Duration TTL = Duration.ofDays(7);
    private static final String VALID_HASH = "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3";

    private Session activeSession;

    @BeforeEach
    void setUp() {
        activeSession = Session.reconstitute(
                1L,
                100L,
                NOW.minus(Duration.ofHours(1)),
                NOW.plus(Duration.ofDays(1)),
                null,
                SessionStatus.ACTIVE
        );
    }

    @Nested
    @DisplayName("Criação de RefreshToken (create)")
    class CreateTests {

        @Test
        @DisplayName("Sucesso: cria RefreshToken ativo com valores válidos")
        void shouldCreateActiveRefreshToken() {
            RefreshToken token = RefreshToken.create(activeSession, VALID_HASH, TTL, NOW);

            assertThat(token).isNotNull();
            assertThat(token.getSession()).isEqualTo(activeSession);
            assertThat(token.getTokenHash()).isEqualTo(VALID_HASH);
            assertThat(token.getCreatedAt()).isEqualTo(NOW);
            assertThat(token.getExpiresAt()).isEqualTo(NOW.plus(TTL));
            assertThat(token.getStatus()).isEqualTo(RefreshTokenStatus.ACTIVE);
            assertThat(token.getReplacedByTokenId()).isNull();
            assertThat(token.isActive(NOW)).isTrue();
            assertThat(token.isValid(NOW)).isTrue();
            assertThat(token.isRevoked()).isFalse();
            assertThat(token.isRotated()).isFalse();
        }

        @Test
        @DisplayName("Falha: session nula")
        void shouldThrowWhenSessionIsNull() {
            assertThatThrownBy(() -> RefreshToken.create(null, VALID_HASH, TTL, NOW))
                    .isInstanceOf(RefreshTokenValidationException.class)
                    .hasMessageContaining("session cannot be null");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t\n"})
        @DisplayName("Falha: tokenHash nulo, vazio ou em branco")
        void shouldThrowWhenTokenHashIsInvalid(String hash) {
            assertThatThrownBy(() -> RefreshToken.create(activeSession, hash, TTL, NOW))
                    .isInstanceOf(RefreshTokenValidationException.class);
        }

        @Test
        @DisplayName("Falha: tokenHash excede 255 caracteres")
        void shouldThrowWhenTokenHashExceedsMaxLength() {
            String longHash = "x".repeat(256);
            assertThatThrownBy(() -> RefreshToken.create(activeSession, longHash, TTL, NOW))
                    .isInstanceOf(RefreshTokenValidationException.class)
                    .hasMessageContaining("cannot exceed 255 characters");
        }

        @Test
        @DisplayName("Falha: TTL nulo, zero ou negativo")
        void shouldThrowWhenTtlIsInvalid() {
            assertThatThrownBy(() -> RefreshToken.create(activeSession, VALID_HASH, null, NOW))
                    .isInstanceOf(RefreshTokenValidationException.class);

            assertThatThrownBy(() -> RefreshToken.create(activeSession, VALID_HASH, Duration.ZERO, NOW))
                    .isInstanceOf(RefreshTokenValidationException.class)
                    .hasMessageContaining("ttl must be greater than zero");

            assertThatThrownBy(() -> RefreshToken.create(activeSession, VALID_HASH, Duration.ofMinutes(-5), NOW))
                    .isInstanceOf(RefreshTokenValidationException.class);
        }

        @Test
        @DisplayName("Falha: now nulo")
        void shouldThrowWhenNowIsNull() {
            assertThatThrownBy(() -> RefreshToken.create(activeSession, VALID_HASH, TTL, null))
                    .isInstanceOf(RefreshTokenValidationException.class)
                    .hasMessageContaining("now cannot be null");
        }
    }

    @Nested
    @DisplayName("Reconstituição de RefreshToken (reconstitute)")
    class ReconstituteTests {

        @Test
        @DisplayName("Sucesso: reconstitui token ACTIVE")
        void shouldReconstituteActiveToken() {
            RefreshToken token = RefreshToken.reconstitute(
                    10L,
                    activeSession,
                    VALID_HASH,
                    NOW.minus(Duration.ofDays(1)),
                    NOW.plus(Duration.ofDays(6)),
                    RefreshTokenStatus.ACTIVE,
                    null
            );

            assertThat(token.getId()).isEqualTo(10L);
            assertThat(token.getStatus()).isEqualTo(RefreshTokenStatus.ACTIVE);
            assertThat(token.getReplacedByTokenId()).isNull();
        }

        @Test
        @DisplayName("Sucesso: reconstitui token ROTATED com replacedByTokenId")
        void shouldReconstituteRotatedToken() {
            RefreshToken token = RefreshToken.reconstitute(
                    10L,
                    activeSession,
                    VALID_HASH,
                    NOW.minus(Duration.ofDays(1)),
                    NOW.plus(Duration.ofDays(6)),
                    RefreshTokenStatus.ROTATED,
                    20L
            );

            assertThat(token.getStatus()).isEqualTo(RefreshTokenStatus.ROTATED);
            assertThat(token.getReplacedByTokenId()).isEqualTo(20L);
            assertThat(token.isRotated()).isTrue();
        }

        @Test
        @DisplayName("Falha: id nulo ou não positivo")
        void shouldThrowWhenIdIsInvalid() {
            assertThatThrownBy(() -> RefreshToken.reconstitute(null, activeSession, VALID_HASH, NOW, NOW.plus(TTL), RefreshTokenStatus.ACTIVE, null))
                    .isInstanceOf(RefreshTokenValidationException.class);

            assertThatThrownBy(() -> RefreshToken.reconstitute(0L, activeSession, VALID_HASH, NOW, NOW.plus(TTL), RefreshTokenStatus.ACTIVE, null))
                    .isInstanceOf(RefreshTokenValidationException.class);
        }

        @Test
        @DisplayName("Falha: expiresAt não é posterior a createdAt")
        void shouldThrowWhenExpiresAtIsNotAfterCreatedAt() {
            assertThatThrownBy(() -> RefreshToken.reconstitute(1L, activeSession, VALID_HASH, NOW, NOW, RefreshTokenStatus.ACTIVE, null))
                    .isInstanceOf(RefreshTokenValidationException.class);

            assertThatThrownBy(() -> RefreshToken.reconstitute(1L, activeSession, VALID_HASH, NOW, NOW.minus(Duration.ofMinutes(1)), RefreshTokenStatus.ACTIVE, null))
                    .isInstanceOf(RefreshTokenValidationException.class);
        }

        @Test
        @DisplayName("Falha de consistência: ACTIVE com replacedByTokenId ou ROTATED sem replacedByTokenId")
        void shouldThrowWhenStatusIsConsistentWithReplacementId() {
            assertThatThrownBy(() -> RefreshToken.reconstitute(1L, activeSession, VALID_HASH, NOW, NOW.plus(TTL), RefreshTokenStatus.ACTIVE, 99L))
                    .isInstanceOf(RefreshTokenValidationException.class)
                    .hasMessageContaining("ACTIVE refresh token cannot have replacedByTokenId");

            assertThatThrownBy(() -> RefreshToken.reconstitute(1L, activeSession, VALID_HASH, NOW, NOW.plus(TTL), RefreshTokenStatus.ROTATED, null))
                    .isInstanceOf(RefreshTokenValidationException.class)
                    .hasMessageContaining("ROTATED refresh token must have replacedByTokenId");

            assertThatThrownBy(() -> RefreshToken.reconstitute(1L, activeSession, VALID_HASH, NOW, NOW.plus(TTL), RefreshTokenStatus.REVOKED, 99L))
                    .isInstanceOf(RefreshTokenValidationException.class)
                    .hasMessageContaining("REVOKED refresh token cannot have replacedByTokenId");
        }
    }

    @Nested
    @DisplayName("Ciclo de Vida: Revogação e Rotação")
    class LifecycleTests {

        @Test
        @DisplayName("Sucesso: revoga token ativo não expirado")
        void shouldRevokeActiveToken() {
            RefreshToken token = RefreshToken.create(activeSession, VALID_HASH, TTL, NOW);

            token.revoke(NOW.plus(Duration.ofHours(1)));

            assertThat(token.getStatus()).isEqualTo(RefreshTokenStatus.REVOKED);
            assertThat(token.isRevoked()).isTrue();
            assertThat(token.isActive(NOW)).isFalse();
            assertThat(token.isValid(NOW)).isFalse();
        }

        @Test
        @DisplayName("Falha: revogar token já revogado lança RefreshTokenStateException")
        void shouldThrowWhenRevokingAlreadyRevokedToken() {
            RefreshToken token = RefreshToken.create(activeSession, VALID_HASH, TTL, NOW);
            token.revoke(NOW.plus(Duration.ofHours(1)));

            assertThatThrownBy(() -> token.revoke(NOW.plus(Duration.ofHours(2))))
                    .isInstanceOf(RefreshTokenStateException.class)
                    .hasMessageContaining("Only active refresh tokens can be revoked");
        }

        @Test
        @DisplayName("Falha: revogar token expirado lança RefreshTokenStateException")
        void shouldThrowWhenRevokingExpiredToken() {
            RefreshToken token = RefreshToken.create(activeSession, VALID_HASH, TTL, NOW);
            Instant afterExpiry = NOW.plus(TTL).plusSeconds(1);

            assertThatThrownBy(() -> token.revoke(afterExpiry))
                    .isInstanceOf(RefreshTokenStateException.class)
                    .hasMessageContaining("Expired refresh tokens cannot be revoked");
        }

        @Test
        @DisplayName("Sucesso: rotaciona token válido gerando status ROTATED")
        void shouldRotateValidToken() {
            RefreshToken token = RefreshToken.create(activeSession, VALID_HASH, TTL, NOW);

            token.rotate(200L, NOW.plus(Duration.ofMinutes(10)));

            assertThat(token.getStatus()).isEqualTo(RefreshTokenStatus.ROTATED);
            assertThat(token.getReplacedByTokenId()).isEqualTo(200L);
            assertThat(token.isRotated()).isTrue();
            assertThat(token.isActive(NOW)).isFalse();
            assertThat(token.isValid(NOW)).isFalse();
        }

        @Test
        @DisplayName("Falha: rotacionar token já rotacionado lança RefreshTokenStateException")
        void shouldThrowWhenRotatingAlreadyRotatedToken() {
            RefreshToken token = RefreshToken.create(activeSession, VALID_HASH, TTL, NOW);
            token.rotate(200L, NOW.plus(Duration.ofMinutes(10)));

            assertThatThrownBy(() -> token.rotate(300L, NOW.plus(Duration.ofMinutes(20))))
                    .isInstanceOf(RefreshTokenStateException.class)
                    .hasMessageContaining("Only valid refresh tokens can be rotated");
        }

        @Test
        @DisplayName("Falha: rotacionar com ID de novo token inválido lança RefreshTokenValidationException")
        void shouldThrowWhenRotatingWithInvalidNewTokenId() {
            RefreshToken token = RefreshToken.create(activeSession, VALID_HASH, TTL, NOW);

            assertThatThrownBy(() -> token.rotate(null, NOW.plus(Duration.ofMinutes(5))))
                    .isInstanceOf(RefreshTokenValidationException.class)
                    .hasMessageContaining("newRefreshTokenId cannot be null");

            assertThatThrownBy(() -> token.rotate(-1L, NOW.plus(Duration.ofMinutes(5))))
                    .isInstanceOf(RefreshTokenValidationException.class)
                    .hasMessageContaining("newRefreshTokenId must be positive");
        }

        @Test
        @DisplayName("Validação: token não é válido se a sessão estiver inativa")
        void shouldBeInvalidWhenSessionIsInvalid() {
            Session expiredSession = Session.reconstitute(
                    2L,
                    100L,
                    NOW.minus(Duration.ofHours(2)),
                    NOW.minus(Duration.ofHours(1)),
                    null,
                    SessionStatus.ACTIVE
            );

            RefreshToken token = RefreshToken.create(expiredSession, VALID_HASH, TTL, NOW);

            assertThat(token.isActive(NOW)).isTrue();
            assertThat(token.isValid(NOW)).isFalse();
        }

        @Test
        @DisplayName("Expiração: isExpired retorna true após e no limite do TTL")
        void shouldCorrectlyDetectExpiration() {
            RefreshToken token = RefreshToken.create(activeSession, VALID_HASH, TTL, NOW);

            assertThat(token.isExpired(NOW.plus(Duration.ofDays(6)))).isFalse();
            assertThat(token.isExpired(NOW.plus(TTL))).isTrue();
            assertThat(token.isExpired(NOW.plus(TTL).plusSeconds(10))).isTrue();
        }
    }
}
