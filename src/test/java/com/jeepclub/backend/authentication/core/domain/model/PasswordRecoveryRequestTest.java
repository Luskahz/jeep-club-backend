package com.jeepclub.backend.authentication.core.domain.model;

import com.jeepclub.backend.iam.authentication.core.domain.enums.PasswordRecoveryRequestMethod;
import com.jeepclub.backend.iam.authentication.core.domain.enums.PasswordRecoveryRequestStatus;
import com.jeepclub.backend.iam.authentication.core.domain.exception.passwordrecovery.PasswordRecoveryRequestStateException;
import com.jeepclub.backend.iam.authentication.core.domain.exception.passwordrecovery.PasswordRecoveryRequestValidationException;
import com.jeepclub.backend.iam.authentication.core.domain.model.PasswordRecoveryRequest;
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

class PasswordRecoveryRequestTest {

    private static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");
    private static final Duration TTL = Duration.ofHours(2);
    private static final Long USER_ID = 10L;
    private static final String VALID_HASH = "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9";

    @Nested
    @DisplayName("Criação de Solicitação Aberta (createOpenRequest)")
    class CreateOpenRequestTests {

        @Test
        @DisplayName("Sucesso: cria solicitação OPEN com método UNDEFINED")
        void shouldCreateOpenRequest() {
            PasswordRecoveryRequest request = PasswordRecoveryRequest.createOpenRequest(
                    USER_ID,
                    NOW,
                    NOW.plus(TTL)
            );

            assertThat(request).isNotNull();
            assertThat(request.getUserId()).isEqualTo(USER_ID);
            assertThat(request.getStatus()).isEqualTo(PasswordRecoveryRequestStatus.OPEN);
            assertThat(request.getMethod()).isEqualTo(PasswordRecoveryRequestMethod.UNDEFINED);
            assertThat(request.getTokenHash()).isNull();
            assertThat(request.getCreatedAt()).isEqualTo(NOW);
            assertThat(request.getExpiresAt()).isEqualTo(NOW.plus(TTL));
            assertThat(request.getResolvedAt()).isNull();
            assertThat(request.getCancelledAt()).isNull();
            assertThat(request.isOpen(NOW)).isTrue();
            assertThat(request.isExpired(NOW)).isFalse();
            assertThat(request.canBeCancelled()).isTrue();
            assertThat(request.hasUndefinedMethod()).isTrue();
        }

        @Test
        @DisplayName("Falha: userId nulo ou não positivo")
        void shouldThrowWhenUserIdIsInvalid() {
            assertThatThrownBy(() -> PasswordRecoveryRequest.createOpenRequest(null, NOW, NOW.plus(TTL)))
                    .isInstanceOf(PasswordRecoveryRequestValidationException.class)
                    .hasMessageContaining("userId cannot be null");

            assertThatThrownBy(() -> PasswordRecoveryRequest.createOpenRequest(0L, NOW, NOW.plus(TTL)))
                    .isInstanceOf(PasswordRecoveryRequestValidationException.class)
                    .hasMessageContaining("userId must be positive");
        }

        @Test
        @DisplayName("Falha: datas nulas ou expiresAt não posterior a createdAt")
        void shouldThrowWhenDatesAreInvalid() {
            assertThatThrownBy(() -> PasswordRecoveryRequest.createOpenRequest(USER_ID, null, NOW.plus(TTL)))
                    .isInstanceOf(PasswordRecoveryRequestValidationException.class);

            assertThatThrownBy(() -> PasswordRecoveryRequest.createOpenRequest(USER_ID, NOW, null))
                    .isInstanceOf(PasswordRecoveryRequestValidationException.class);

            assertThatThrownBy(() -> PasswordRecoveryRequest.createOpenRequest(USER_ID, NOW, NOW))
                    .isInstanceOf(PasswordRecoveryRequestValidationException.class)
                    .hasMessageContaining("expiresAt must be after createdAt");
        }
    }

    @Nested
    @DisplayName("Definição de Método de Recuperação")
    class MethodAssignmentTests {

        @Test
        @DisplayName("Sucesso: altera para método EMAIL_TOKEN com hash válido")
        void shouldChangeToEmailTokenMethod() {
            PasswordRecoveryRequest request = PasswordRecoveryRequest.createOpenRequest(USER_ID, NOW, NOW.plus(TTL));

            request.changeToEmailTokenMethod(VALID_HASH, NOW);

            assertThat(request.getMethod()).isEqualTo(PasswordRecoveryRequestMethod.EMAIL_TOKEN);
            assertThat(request.getTokenHash()).isEqualTo(VALID_HASH);
            assertThat(request.isTokenBased()).isTrue();
            assertThat(request.isTemporaryPasswordBased()).isFalse();
        }

        @Test
        @DisplayName("Sucesso: altera para método ADMIN_RESET_LINK com hash")
        void shouldChangeToAdminResetLinkMethod() {
            PasswordRecoveryRequest request = PasswordRecoveryRequest.createOpenRequest(USER_ID, NOW, NOW.plus(TTL));

            request.changeToAdminResetLinkMethod(VALID_HASH, NOW);

            assertThat(request.getMethod()).isEqualTo(PasswordRecoveryRequestMethod.ADMIN_RESET_LINK);
            assertThat(request.getTokenHash()).isEqualTo(VALID_HASH);
            assertThat(request.isTokenBased()).isTrue();
        }

        @Test
        @DisplayName("Sucesso: altera para método ADMIN_TEMPORARY_PASSWORD sem hash")
        void shouldChangeToAdminTemporaryPasswordMethod() {
            PasswordRecoveryRequest request = PasswordRecoveryRequest.createOpenRequest(USER_ID, NOW, NOW.plus(TTL));

            request.changeToAdminTemporaryPasswordMethod(NOW);

            assertThat(request.getMethod()).isEqualTo(PasswordRecoveryRequestMethod.ADMIN_TEMPORARY_PASSWORD);
            assertThat(request.getTokenHash()).isNull();
            assertThat(request.isTemporaryPasswordBased()).isTrue();
            assertThat(request.isTokenBased()).isFalse();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "\t"})
        @DisplayName("Falha: hash nulo ou em branco para EMAIL_TOKEN")
        void shouldThrowWhenTokenHashIsInvalidForEmailToken(String hash) {
            PasswordRecoveryRequest request = PasswordRecoveryRequest.createOpenRequest(USER_ID, NOW, NOW.plus(TTL));

            assertThatThrownBy(() -> request.changeToEmailTokenMethod(hash, NOW))
                    .isInstanceOf(PasswordRecoveryRequestValidationException.class);
        }

        @Test
        @DisplayName("Falha: alterar método em solicitação expirada lança PasswordRecoveryRequestStateException")
        void shouldThrowWhenChangingMethodOnExpiredRequest() {
            PasswordRecoveryRequest request = PasswordRecoveryRequest.createOpenRequest(USER_ID, NOW, NOW.plus(TTL));
            Instant afterExpiry = NOW.plus(TTL).plusSeconds(1);

            assertThatThrownBy(() -> request.changeToEmailTokenMethod(VALID_HASH, afterExpiry))
                    .isInstanceOf(PasswordRecoveryRequestStateException.class)
                    .hasMessageContaining("not open or has expired");
        }
    }

    @Nested
    @DisplayName("Resolução e Cancelamento (resolve e cancel)")
    class ResolutionAndCancellationTests {

        @Test
        @DisplayName("Sucesso: resolve solicitação aberta com método definido")
        void shouldResolveOpenRequest() {
            PasswordRecoveryRequest request = PasswordRecoveryRequest.createOpenRequest(USER_ID, NOW, NOW.plus(TTL));
            request.changeToEmailTokenMethod(VALID_HASH, NOW);
            Instant resolveInstant = NOW.plus(Duration.ofMinutes(30));

            request.resolve(resolveInstant);

            assertThat(request.getStatus()).isEqualTo(PasswordRecoveryRequestStatus.RESOLVED);
            assertThat(request.getResolvedAt()).isEqualTo(resolveInstant);
            assertThat(request.isOpen(resolveInstant)).isFalse();
            assertThat(request.canBeCancelled()).isFalse();
        }

        @Test
        @DisplayName("Falha: tentar resolver sem definir método lança PasswordRecoveryRequestStateException")
        void shouldThrowWhenResolvingWithUndefinedMethod() {
            PasswordRecoveryRequest request = PasswordRecoveryRequest.createOpenRequest(USER_ID, NOW, NOW.plus(TTL));

            assertThatThrownBy(() -> request.resolve(NOW.plus(Duration.ofMinutes(10))))
                    .isInstanceOf(PasswordRecoveryRequestStateException.class)
                    .hasMessageContaining("method must be defined before resolution");
        }

        @Test
        @DisplayName("Falha: tentar resolver solicitação já resolvida")
        void shouldThrowWhenResolvingAlreadyResolvedRequest() {
            PasswordRecoveryRequest request = PasswordRecoveryRequest.createOpenRequest(USER_ID, NOW, NOW.plus(TTL));
            request.changeToAdminTemporaryPasswordMethod(NOW);
            request.resolve(NOW.plus(Duration.ofMinutes(10)));

            assertThatThrownBy(() -> request.resolve(NOW.plus(Duration.ofMinutes(20))))
                    .isInstanceOf(PasswordRecoveryRequestStateException.class);
        }

        @Test
        @DisplayName("Sucesso: cancela solicitação aberta")
        void shouldCancelOpenRequest() {
            PasswordRecoveryRequest request = PasswordRecoveryRequest.createOpenRequest(USER_ID, NOW, NOW.plus(TTL));
            Instant cancelInstant = NOW.plus(Duration.ofMinutes(15));

            request.cancel(cancelInstant);

            assertThat(request.getStatus()).isEqualTo(PasswordRecoveryRequestStatus.CANCELLED);
            assertThat(request.getCancelledAt()).isEqualTo(cancelInstant);
            assertThat(request.isOpen(cancelInstant)).isFalse();
            assertThat(request.canBeCancelled()).isFalse();
        }

        @Test
        @DisplayName("Idempotência: cancel em solicitação não aberta não altera estado")
        void shouldBeIdempotentWhenCancellingNonOpenRequest() {
            PasswordRecoveryRequest request = PasswordRecoveryRequest.createOpenRequest(USER_ID, NOW, NOW.plus(TTL));
            request.changeToAdminTemporaryPasswordMethod(NOW);
            request.resolve(NOW.plus(Duration.ofMinutes(10)));

            request.cancel(NOW.plus(Duration.ofMinutes(20)));

            assertThat(request.getStatus()).isEqualTo(PasswordRecoveryRequestStatus.RESOLVED);
            assertThat(request.getCancelledAt()).isNull();
        }
    }

    @Nested
    @DisplayName("Reconstituição de PasswordRecoveryRequest")
    class ReconstituteTests {

        @Test
        @DisplayName("Sucesso: reconstitui solicitação RESOLVED")
        void shouldReconstituteResolvedRequest() {
            Instant created = NOW.minus(Duration.ofHours(1));
            Instant resolved = NOW.minus(Duration.ofMinutes(30));
            Instant expires = NOW.plus(Duration.ofHours(1));

            PasswordRecoveryRequest request = PasswordRecoveryRequest.reconstitute(
                    5L,
                    USER_ID,
                    VALID_HASH,
                    expires,
                    created,
                    resolved,
                    null,
                    PasswordRecoveryRequestStatus.RESOLVED,
                    PasswordRecoveryRequestMethod.EMAIL_TOKEN
            );

            assertThat(request.getId()).isEqualTo(5L);
            assertThat(request.getStatus()).isEqualTo(PasswordRecoveryRequestStatus.RESOLVED);
            assertThat(request.getResolvedAt()).isEqualTo(resolved);
        }

        @Test
        @DisplayName("Falha: id inválido")
        void shouldThrowWhenIdIsInvalid() {
            assertThatThrownBy(() -> PasswordRecoveryRequest.reconstitute(
                    null, USER_ID, VALID_HASH, NOW.plus(TTL), NOW, null, null,
                    PasswordRecoveryRequestStatus.OPEN, PasswordRecoveryRequestMethod.EMAIL_TOKEN
            )).isInstanceOf(PasswordRecoveryRequestValidationException.class);
        }

        @Test
        @DisplayName("Falha: método token-based sem tokenHash")
        void shouldThrowWhenTokenBasedMethodHasNoHash() {
            assertThatThrownBy(() -> PasswordRecoveryRequest.reconstitute(
                    1L, USER_ID, null, NOW.plus(TTL), NOW, null, null,
                    PasswordRecoveryRequestStatus.OPEN, PasswordRecoveryRequestMethod.EMAIL_TOKEN
            )).isInstanceOf(PasswordRecoveryRequestValidationException.class)
                    .hasMessageContaining("tokenHash is required for token-based");
        }

        @Test
        @DisplayName("Falha: método sem token com tokenHash presente")
        void shouldThrowWhenNonTokenMethodHasHash() {
            assertThatThrownBy(() -> PasswordRecoveryRequest.reconstitute(
                    1L, USER_ID, VALID_HASH, NOW.plus(TTL), NOW, null, null,
                    PasswordRecoveryRequestStatus.OPEN, PasswordRecoveryRequestMethod.ADMIN_TEMPORARY_PASSWORD
            )).isInstanceOf(PasswordRecoveryRequestValidationException.class)
                    .hasMessageContaining("tokenHash must be null for non-token");
        }
    }
}
