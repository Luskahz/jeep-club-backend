package com.jeepclub.backend.authentication.core.domain.model;

import com.jeepclub.backend.iam.authentication.core.domain.enums.SessionStatus;
import com.jeepclub.backend.iam.authentication.core.domain.exception.session.SessionInvalidActiveStateException;
import com.jeepclub.backend.iam.authentication.core.domain.exception.session.SessionInvalidExpirationDateException;
import com.jeepclub.backend.iam.authentication.core.domain.exception.session.SessionInvalidLogoutStateException;
import com.jeepclub.backend.iam.authentication.core.domain.exception.session.SessionInvalidRevokeStateException;
import com.jeepclub.backend.iam.authentication.core.domain.exception.session.SessionInvalidTtlValueException;
import com.jeepclub.backend.iam.authentication.core.domain.exception.session.SessionMissingCreatedAtException;
import com.jeepclub.backend.iam.authentication.core.domain.exception.session.SessionMissingExpiresAtException;
import com.jeepclub.backend.iam.authentication.core.domain.exception.session.SessionMissingIdException;
import com.jeepclub.backend.iam.authentication.core.domain.exception.session.SessionMissingStatusException;
import com.jeepclub.backend.iam.authentication.core.domain.exception.session.SessionMissingTtlException;
import com.jeepclub.backend.iam.authentication.core.domain.exception.session.SessionMissingUserIdException;
import com.jeepclub.backend.iam.authentication.core.domain.exception.session.SessionNotActiveException;
import com.jeepclub.backend.iam.authentication.core.domain.exception.session.SessionNowInstantRequiredException;
import com.jeepclub.backend.iam.authentication.core.domain.model.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionTest {

    private static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");
    private static final Duration TTL = Duration.ofHours(8);
    private static final Long USER_ID = 42L;

    @Nested
    @DisplayName("Criação de Sessão (create)")
    class CreateTests {

        @Test
        @DisplayName("Sucesso: cria sessão ativa")
        void shouldCreateActiveSession() {
            Session session = Session.create(USER_ID, TTL, NOW);

            assertThat(session).isNotNull();
            assertThat(session.getUserId()).isEqualTo(USER_ID);
            assertThat(session.getCreatedAt()).isEqualTo(NOW);
            assertThat(session.getExpiresAt()).isEqualTo(NOW.plus(TTL));
            assertThat(session.getStatus()).isEqualTo(SessionStatus.ACTIVE);
            assertThat(session.getLoggedOutAt()).isNull();
            assertThat(session.isActive(NOW)).isTrue();
            assertThat(session.isValid(NOW)).isTrue();
            assertThat(session.isLoggedOut()).isFalse();
            assertThat(session.isRevoked()).isFalse();
        }

        @Test
        @DisplayName("Falha: userId nulo")
        void shouldThrowWhenUserIdIsNull() {
            assertThatThrownBy(() -> Session.create(null, TTL, NOW))
                    .isInstanceOf(SessionMissingUserIdException.class)
                    .hasMessageContaining("userId is required");
        }

        @Test
        @DisplayName("Falha: TTL nulo")
        void shouldThrowWhenTtlIsNull() {
            assertThatThrownBy(() -> Session.create(USER_ID, null, NOW))
                    .isInstanceOf(SessionMissingTtlException.class)
                    .hasMessageContaining("ttl is required");
        }

        @Test
        @DisplayName("Falha: TTL zero ou negativo")
        void shouldThrowWhenTtlIsNonPositive() {
            assertThatThrownBy(() -> Session.create(USER_ID, Duration.ZERO, NOW))
                    .isInstanceOf(SessionInvalidTtlValueException.class)
                    .hasMessageContaining("ttl must be positive");

            assertThatThrownBy(() -> Session.create(USER_ID, Duration.ofMinutes(-1), NOW))
                    .isInstanceOf(SessionInvalidTtlValueException.class);
        }

        @Test
        @DisplayName("Falha: now nulo")
        void shouldThrowWhenNowIsNull() {
            assertThatThrownBy(() -> Session.create(USER_ID, TTL, null))
                    .isInstanceOf(SessionNowInstantRequiredException.class)
                    .hasMessageContaining("now is required");
        }
    }

    @Nested
    @DisplayName("Reconstituição de Sessão (reconstitute)")
    class ReconstituteTests {

        @Test
        @DisplayName("Sucesso: reconstitui sessão ativa")
        void shouldReconstituteActiveSession() {
            Session session = Session.reconstitute(
                    1L,
                    USER_ID,
                    NOW.minus(Duration.ofHours(1)),
                    NOW.plus(Duration.ofHours(7)),
                    null,
                    SessionStatus.ACTIVE
            );

            assertThat(session.getId()).isEqualTo(1L);
            assertThat(session.getStatus()).isEqualTo(SessionStatus.ACTIVE);
            assertThat(session.getLoggedOutAt()).isNull();
        }

        @Test
        @DisplayName("Sucesso: reconstitui sessão encerrada (LOGGED_OUT)")
        void shouldReconstituteLoggedOutSession() {
            Instant logoutTime = NOW.minus(Duration.ofMinutes(30));
            Session session = Session.reconstitute(
                    1L,
                    USER_ID,
                    NOW.minus(Duration.ofHours(2)),
                    NOW.plus(Duration.ofHours(6)),
                    logoutTime,
                    SessionStatus.LOGGED_OUT
            );

            assertThat(session.getStatus()).isEqualTo(SessionStatus.LOGGED_OUT);
            assertThat(session.getLoggedOutAt()).isEqualTo(logoutTime);
            assertThat(session.isLoggedOut()).isTrue();
            assertThat(session.isActive(NOW)).isFalse();
        }

        @Test
        @DisplayName("Sucesso: reconstitui sessão revogada (REVOKED)")
        void shouldReconstituteRevokedSession() {
            Session session = Session.reconstitute(
                    1L,
                    USER_ID,
                    NOW.minus(Duration.ofHours(2)),
                    NOW.plus(Duration.ofHours(6)),
                    null,
                    SessionStatus.REVOKED
            );

            assertThat(session.getStatus()).isEqualTo(SessionStatus.REVOKED);
            assertThat(session.isRevoked()).isTrue();
            assertThat(session.isActive(NOW)).isFalse();
        }

        @Test
        @DisplayName("Falha: campos obrigatórios ausentes")
        void shouldThrowWhenRequiredFieldsMissing() {
            Instant created = NOW.minus(Duration.ofHours(1));
            Instant expires = NOW.plus(Duration.ofHours(1));

            assertThatThrownBy(() -> Session.reconstitute(null, USER_ID, created, expires, null, SessionStatus.ACTIVE))
                    .isInstanceOf(SessionMissingIdException.class);

            assertThatThrownBy(() -> Session.reconstitute(1L, null, created, expires, null, SessionStatus.ACTIVE))
                    .isInstanceOf(SessionMissingUserIdException.class);

            assertThatThrownBy(() -> Session.reconstitute(1L, USER_ID, null, expires, null, SessionStatus.ACTIVE))
                    .isInstanceOf(SessionMissingCreatedAtException.class);

            assertThatThrownBy(() -> Session.reconstitute(1L, USER_ID, created, null, null, SessionStatus.ACTIVE))
                    .isInstanceOf(SessionMissingExpiresAtException.class);

            assertThatThrownBy(() -> Session.reconstitute(1L, USER_ID, created, expires, null, null))
                    .isInstanceOf(SessionMissingStatusException.class);
        }

        @Test
        @DisplayName("Falha: expiresAt não é posterior a createdAt")
        void shouldThrowWhenExpiresAtNotAfterCreatedAt() {
            Instant created = NOW;
            assertThatThrownBy(() -> Session.reconstitute(1L, USER_ID, created, created, null, SessionStatus.ACTIVE))
                    .isInstanceOf(SessionInvalidExpirationDateException.class);
        }

        @Test
        @DisplayName("Falha: ACTIVE com loggedOutAt preenchido")
        void shouldThrowWhenActiveSessionHasLoggedOutAt() {
            Instant created = NOW.minus(Duration.ofHours(1));
            Instant expires = NOW.plus(Duration.ofHours(1));

            assertThatThrownBy(() -> Session.reconstitute(1L, USER_ID, created, expires, NOW, SessionStatus.ACTIVE))
                    .isInstanceOf(SessionInvalidActiveStateException.class);
        }

        @Test
        @DisplayName("Falha: LOGGED_OUT sem loggedOutAt ou anterior a createdAt")
        void shouldThrowWhenLoggedOutStateIsInconsistent() {
            Instant created = NOW.minus(Duration.ofHours(1));
            Instant expires = NOW.plus(Duration.ofHours(1));

            assertThatThrownBy(() -> Session.reconstitute(1L, USER_ID, created, expires, null, SessionStatus.LOGGED_OUT))
                    .isInstanceOf(SessionInvalidLogoutStateException.class);

            assertThatThrownBy(() -> Session.reconstitute(1L, USER_ID, created, expires, created.minusSeconds(10), SessionStatus.LOGGED_OUT))
                    .isInstanceOf(SessionInvalidLogoutStateException.class);
        }

        @Test
        @DisplayName("Falha: REVOKED com loggedOutAt preenchido")
        void shouldThrowWhenRevokedSessionHasLoggedOutAt() {
            Instant created = NOW.minus(Duration.ofHours(1));
            Instant expires = NOW.plus(Duration.ofHours(1));

            assertThatThrownBy(() -> Session.reconstitute(1L, USER_ID, created, expires, NOW, SessionStatus.REVOKED))
                    .isInstanceOf(SessionInvalidRevokeStateException.class);
        }
    }

    @Nested
    @DisplayName("Transições de Estado e Expiração")
    class StateTransitionTests {

        @Test
        @DisplayName("Sucesso: encerra sessão com logout()")
        void shouldLogoutActiveSession() {
            Session session = Session.create(USER_ID, TTL, NOW);
            Instant logoutInstant = NOW.plus(Duration.ofHours(2));

            session.logout(logoutInstant);

            assertThat(session.getStatus()).isEqualTo(SessionStatus.LOGGED_OUT);
            assertThat(session.getLoggedOutAt()).isEqualTo(logoutInstant);
            assertThat(session.isLoggedOut()).isTrue();
            assertThat(session.isActive(logoutInstant)).isFalse();
        }

        @Test
        @DisplayName("Idempotência: logout em sessão já inativa não sobrescreve loggedOutAt")
        void shouldBeIdempotentWhenLoggingOutNonActiveSession() {
            Session session = Session.create(USER_ID, TTL, NOW);
            Instant firstLogout = NOW.plus(Duration.ofHours(1));
            session.logout(firstLogout);

            session.logout(NOW.plus(Duration.ofHours(3)));

            assertThat(session.getLoggedOutAt()).isEqualTo(firstLogout);
        }

        @Test
        @DisplayName("Sucesso: revoga sessão ativa com revoke()")
        void shouldRevokeActiveSession() {
            Session session = Session.create(USER_ID, TTL, NOW);

            session.revoke();

            assertThat(session.getStatus()).isEqualTo(SessionStatus.REVOKED);
            assertThat(session.isRevoked()).isTrue();
            assertThat(session.isActive(NOW)).isFalse();
        }

        @Test
        @DisplayName("Falha: revogar sessão não ativa lança SessionNotActiveException")
        void shouldThrowWhenRevokingNonActiveSession() {
            Session session = Session.create(USER_ID, TTL, NOW);
            session.revoke();

            assertThatThrownBy(session::revoke)
                    .isInstanceOf(SessionNotActiveException.class)
                    .hasMessageContaining("Only active sessions can be revoked");
        }

        @Test
        @DisplayName("Expiração: valida corretamente isNotExpired")
        void shouldValidateExpirationCorrectly() {
            Session session = Session.create(USER_ID, TTL, NOW);

            assertThat(session.isNotExpired(NOW.plus(Duration.ofHours(7)))).isTrue();
            assertThat(session.isNotExpired(NOW.plus(TTL))).isFalse();
            assertThat(session.isNotExpired(NOW.plus(TTL).plusSeconds(1))).isFalse();

            assertThatThrownBy(() -> session.isNotExpired(null))
                    .isInstanceOf(SessionNowInstantRequiredException.class);
        }
    }
}
