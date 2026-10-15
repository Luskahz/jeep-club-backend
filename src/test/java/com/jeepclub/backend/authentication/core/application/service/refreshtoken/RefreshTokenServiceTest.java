package com.jeepclub.backend.authentication.core.application.service.refreshtoken;

import com.jeepclub.backend.iam.authentication.core.application.exceptions.refreshtoken.RefreshTokenInvalidException;
import com.jeepclub.backend.iam.authentication.core.application.result.AuthTokens;
import com.jeepclub.backend.iam.authentication.core.application.result.IssuedRefreshToken;
import com.jeepclub.backend.iam.authentication.core.application.service.internal.RefreshTokenIssuanceService;
import com.jeepclub.backend.iam.authentication.core.application.service.refreshtoken.RefreshTokenService;
import com.jeepclub.backend.iam.authentication.core.domain.enums.RefreshTokenStatus;
import com.jeepclub.backend.iam.authentication.core.domain.enums.SessionStatus;
import com.jeepclub.backend.iam.authentication.core.domain.model.AuthenticationAccount;
import com.jeepclub.backend.iam.authentication.core.domain.model.IssuedAccessToken;
import com.jeepclub.backend.iam.authentication.core.domain.model.RefreshToken;
import com.jeepclub.backend.iam.authentication.core.domain.model.Session;
import com.jeepclub.backend.iam.authentication.core.port.JwtService;
import com.jeepclub.backend.iam.authentication.core.port.RefreshTokenHashService;
import com.jeepclub.backend.iam.authentication.core.repository.AuthenticationAccountRepository;
import com.jeepclub.backend.iam.authentication.core.repository.RefreshTokenRepository;
import com.jeepclub.backend.iam.authentication.core.repository.SessionRepository;
import com.jeepclub.backend.iam.identity.api.module.UserDetails;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");
    private static final Long USER_ID = 100L;
    private static final Long SESSION_ID = 1L;
    private static final String RAW_REFRESH_TOKEN = "raw-refresh-token-xyz";
    private static final String TOKEN_HASH = "hashed-refresh-token-xyz";

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private AuthenticationAccountRepository accountRepository;

    @Mock
    private UserQuery identityQuery;

    @Mock
    private RefreshTokenHashService tokenHashService;

    @Mock
    private RefreshTokenIssuanceService refreshTokenIssuanceService;

    @Mock
    private JwtService jwtService;

    private Clock clock;
    private RefreshTokenService refreshTokenService;

    private Session validSession;
    private RefreshToken existingActiveToken;
    private AuthenticationAccount validAccount;
    private UserDetails activeIdentity;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(NOW, ZoneOffset.UTC);
        refreshTokenService = new RefreshTokenService(
                refreshTokenRepository,
                sessionRepository,
                accountRepository,
                identityQuery,
                tokenHashService,
                refreshTokenIssuanceService,
                jwtService,
                clock
        );

        validSession = Session.reconstitute(
                SESSION_ID,
                USER_ID,
                NOW.minus(Duration.ofHours(1)),
                NOW.plus(Duration.ofHours(7)),
                null,
                SessionStatus.ACTIVE
        );

        existingActiveToken = RefreshToken.reconstitute(
                10L,
                validSession,
                TOKEN_HASH,
                NOW.minus(Duration.ofHours(1)),
                NOW.plus(Duration.ofDays(7)),
                RefreshTokenStatus.ACTIVE,
                null
        );

        validAccount = AuthenticationAccount.create(
                USER_ID,
                "$2a$10$validPasswordHashForTestingAccountOnly",
                NOW.minus(Duration.ofDays(30))
        );

        activeIdentity = new UserDetails(
                USER_ID,
                "Fulano de Tal",
                LocalDate.of(1990, 1, 1),
                "fulano@example.com",
                "52998224725",
                "MG-123456",
                "31999999999",
                null,
                true,
                NOW.minus(Duration.ofDays(30)),
                null,
                NOW.minus(Duration.ofDays(30))
        );
    }

    @Test
    @DisplayName("Sucesso: rotação atômica emite novos tokens e marca token anterior como ROTATED")
    void shouldRotateTokensSuccessfully() {
        when(tokenHashService.hash(RAW_REFRESH_TOKEN)).thenReturn(TOKEN_HASH);
        when(refreshTokenRepository.findSessionIdByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(SESSION_ID));
        when(sessionRepository.findUserIdById(SESSION_ID)).thenReturn(Optional.of(USER_ID));
        when(accountRepository.findByIdentityIdForUpdate(USER_ID)).thenReturn(Optional.of(validAccount));
        when(sessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.of(validSession));
        when(refreshTokenRepository.findByTokenHashForUpdate(TOKEN_HASH)).thenReturn(Optional.of(existingActiveToken));
        when(identityQuery.findById(USER_ID)).thenReturn(Optional.of(activeIdentity));

        RefreshToken newRefreshToken = RefreshToken.reconstitute(
                20L,
                validSession,
                "new-token-hash",
                NOW,
                NOW.plus(Duration.ofDays(7)),
                RefreshTokenStatus.ACTIVE,
                null
        );
        IssuedRefreshToken issuedRefreshToken = new IssuedRefreshToken(newRefreshToken, "new-raw-refresh-token");
        when(refreshTokenIssuanceService.issue(eq(validSession), eq(NOW))).thenReturn(issuedRefreshToken);

        IssuedAccessToken issuedAccessToken = new IssuedAccessToken("new-access-token-jwt", NOW.plusSeconds(3600));
        when(jwtService.generateAccessToken(eq(USER_ID), eq("Fulano de Tal"), eq(validSession)))
                .thenReturn(issuedAccessToken);

        AuthTokens result = refreshTokenService.rotate(RAW_REFRESH_TOKEN);

        assertThat(result).isNotNull();
        assertThat(result.refreshToken()).isEqualTo("new-raw-refresh-token");
        assertThat(result.accessToken()).isEqualTo("new-access-token-jwt");
        assertThat(result.expiresInSeconds()).isEqualTo(3600L);

        assertThat(existingActiveToken.isRotated()).isTrue();
        assertThat(existingActiveToken.getReplacedByTokenId()).isEqualTo(20L);
        verify(refreshTokenRepository).save(existingActiveToken);
    }

    @Test
    @DisplayName("Falha: token não encontrado no repositório lança RefreshTokenInvalidException")
    void shouldThrowWhenTokenNotFound() {
        when(tokenHashService.hash(RAW_REFRESH_TOKEN)).thenReturn(TOKEN_HASH);
        when(refreshTokenRepository.findSessionIdByTokenHash(TOKEN_HASH)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.rotate(RAW_REFRESH_TOKEN))
                .isInstanceOf(RefreshTokenInvalidException.class);
    }

    @Test
    @DisplayName("Falha: usuário associado não está administrativamente ativo")
    void shouldThrowWhenIdentityIsNotAdministrativelyActive() {
        UserDetails inactiveIdentity = new UserDetails(
                USER_ID,
                "Inativo",
                LocalDate.of(1990, 1, 1),
                "inativo@example.com",
                "52998224725",
                "MG-123456",
                "31999999999",
                null,
                false, // administrativelyActive = false
                NOW.minus(Duration.ofDays(30)),
                NOW.minus(Duration.ofDays(1)),
                NOW.minus(Duration.ofDays(1))
        );

        when(tokenHashService.hash(RAW_REFRESH_TOKEN)).thenReturn(TOKEN_HASH);
        when(refreshTokenRepository.findSessionIdByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(SESSION_ID));
        when(sessionRepository.findUserIdById(SESSION_ID)).thenReturn(Optional.of(USER_ID));
        when(accountRepository.findByIdentityIdForUpdate(USER_ID)).thenReturn(Optional.of(validAccount));
        when(sessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.of(validSession));
        when(refreshTokenRepository.findByTokenHashForUpdate(TOKEN_HASH)).thenReturn(Optional.of(existingActiveToken));
        when(identityQuery.findById(USER_ID)).thenReturn(Optional.of(inactiveIdentity));

        assertThatThrownBy(() -> refreshTokenService.rotate(RAW_REFRESH_TOKEN))
                .isInstanceOf(RefreshTokenInvalidException.class);
    }

    @Test
    @DisplayName("Falha: conta bloqueada por tentativas inválidas de login impede renovação")
    void shouldThrowWhenAccountIsLocked() {
        for (int i = 0; i < 5; i++) {
            validAccount.registerFailedLogin();
        }
        assertThat(validAccount.isLocked()).isTrue();

        when(tokenHashService.hash(RAW_REFRESH_TOKEN)).thenReturn(TOKEN_HASH);
        when(refreshTokenRepository.findSessionIdByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(SESSION_ID));
        when(sessionRepository.findUserIdById(SESSION_ID)).thenReturn(Optional.of(USER_ID));
        when(accountRepository.findByIdentityIdForUpdate(USER_ID)).thenReturn(Optional.of(validAccount));
        when(sessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.of(validSession));
        when(refreshTokenRepository.findByTokenHashForUpdate(TOKEN_HASH)).thenReturn(Optional.of(existingActiveToken));
        when(identityQuery.findById(USER_ID)).thenReturn(Optional.of(activeIdentity));

        assertThatThrownBy(() -> refreshTokenService.rotate(RAW_REFRESH_TOKEN))
                .isInstanceOf(RefreshTokenInvalidException.class);
    }

    @Test
    @DisplayName("Falha: sessão associada expirou ou foi revogada")
    void shouldThrowWhenSessionIsNotValid() {
        validSession.revoke();
        assertThat(validSession.isValid(NOW)).isFalse();

        when(tokenHashService.hash(RAW_REFRESH_TOKEN)).thenReturn(TOKEN_HASH);
        when(refreshTokenRepository.findSessionIdByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(SESSION_ID));
        when(sessionRepository.findUserIdById(SESSION_ID)).thenReturn(Optional.of(USER_ID));
        when(accountRepository.findByIdentityIdForUpdate(USER_ID)).thenReturn(Optional.of(validAccount));
        when(sessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.of(validSession));
        when(refreshTokenRepository.findByTokenHashForUpdate(TOKEN_HASH)).thenReturn(Optional.of(existingActiveToken));
        when(identityQuery.findById(USER_ID)).thenReturn(Optional.of(activeIdentity));

        assertThatThrownBy(() -> refreshTokenService.rotate(RAW_REFRESH_TOKEN))
                .isInstanceOf(RefreshTokenInvalidException.class);
    }

    @Test
    @DisplayName("Falha: refresh token já foi rotacionado anteriormente")
    void shouldThrowWhenRefreshTokenAlreadyRotated() {
        existingActiveToken.rotate(99L, NOW.minus(Duration.ofMinutes(10)));
        assertThat(existingActiveToken.isValid(NOW)).isFalse();

        when(tokenHashService.hash(RAW_REFRESH_TOKEN)).thenReturn(TOKEN_HASH);
        when(refreshTokenRepository.findSessionIdByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(SESSION_ID));
        when(sessionRepository.findUserIdById(SESSION_ID)).thenReturn(Optional.of(USER_ID));
        when(accountRepository.findByIdentityIdForUpdate(USER_ID)).thenReturn(Optional.of(validAccount));
        when(sessionRepository.findByIdForUpdate(SESSION_ID)).thenReturn(Optional.of(validSession));
        when(refreshTokenRepository.findByTokenHashForUpdate(TOKEN_HASH)).thenReturn(Optional.of(existingActiveToken));
        when(identityQuery.findById(USER_ID)).thenReturn(Optional.of(activeIdentity));

        assertThatThrownBy(() -> refreshTokenService.rotate(RAW_REFRESH_TOKEN))
                .isInstanceOf(RefreshTokenInvalidException.class);
    }
}
