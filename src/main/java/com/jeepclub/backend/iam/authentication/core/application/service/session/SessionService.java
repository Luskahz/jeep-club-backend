package com.jeepclub.backend.iam.authentication.core.application.service.session;

import com.jeepclub.backend.iam.authentication.core.application.exceptions.login.InvalidCredentialsException;
import com.jeepclub.backend.iam.authentication.core.application.exceptions.login.PasswordChangeNotRequiredException;
import com.jeepclub.backend.iam.authentication.core.application.exceptions.login.PasswordChangeChallengeInvalidException;
import com.jeepclub.backend.iam.authentication.core.application.exceptions.login.PasswordRecoveryRequestNotFoundException;
import com.jeepclub.backend.iam.authentication.core.application.exceptions.account.AuthenticationAccountNotFoundException;
import com.jeepclub.backend.iam.authentication.core.application.exceptions.session.SessionNotFoundException;
import com.jeepclub.backend.iam.authentication.core.application.exceptions.session.SessionUserMismatchException;
import com.jeepclub.backend.iam.authentication.core.application.result.AuthTokens;
import com.jeepclub.backend.iam.authentication.core.application.result.MeResult;
import com.jeepclub.backend.iam.authentication.core.application.result.login.AuthenticatedLoginResult;
import com.jeepclub.backend.iam.authentication.core.application.result.login.LoginResult;
import com.jeepclub.backend.iam.authentication.core.application.service.internal.CredentialRevocationService;
import com.jeepclub.backend.iam.authentication.core.application.service.internal.PasswordChangeChallengeIssuer;
import com.jeepclub.backend.iam.authentication.core.application.service.internal.TokenIssuanceService;
import com.jeepclub.backend.iam.authentication.core.domain.enums.PasswordRecoveryRequestMethod;
import com.jeepclub.backend.iam.authentication.core.domain.model.PasswordChangeChallenge;
import com.jeepclub.backend.iam.authentication.core.domain.model.PasswordRecoveryRequest;
import com.jeepclub.backend.iam.authentication.core.domain.model.Session;
import com.jeepclub.backend.iam.authentication.core.domain.model.AuthenticationAccount;
import com.jeepclub.backend.iam.authentication.core.domain.exception.account.AuthenticationAccountBlockedException;
import com.jeepclub.backend.iam.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.iam.authentication.core.port.RefreshTokenHashService;
import com.jeepclub.backend.iam.authentication.core.repository.PasswordChangeChallengeRepository;
import com.jeepclub.backend.iam.authentication.core.repository.PasswordRecoveryRequestRepository;
import com.jeepclub.backend.iam.authentication.core.repository.RefreshTokenRepository;
import com.jeepclub.backend.iam.authentication.core.repository.SessionRepository;
import com.jeepclub.backend.iam.authentication.core.repository.AuthenticationAccountRepository;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final AuthenticationAccountRepository accountRepository;
    private final SessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordChangeChallengeRepository challengeRepository;
    private final PasswordRecoveryRequestRepository recoveryRequestRepository;
    private final PasswordHasher passwordHasher;
    private final RefreshTokenHashService tokenHashService;
    private final CredentialRevocationService credentialRevocationService;
    private final PasswordChangeChallengeIssuer challengeIssuer;
    private final TokenIssuanceService tokenIssuanceService;
    private final UserQuery userQuery;
    private final Clock clock;

    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public LoginResult login(String cpf, String password) {
        Instant now = Instant.now(clock);
        var identity = userQuery.findByCpf(cpf)
                .orElseThrow(InvalidCredentialsException::new);
        AuthenticationAccount account = accountRepository
                .findByIdentityIdForUpdate(identity.id())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordHasher.matches(password, account.getPasswordHash())) {
            account.registerFailedLogin();
            accountRepository.save(account);
            throw new InvalidCredentialsException();
        }

        account.assertCanAttemptLogin();
        if (!identity.administrativelyActive()) {
            throw new AuthenticationAccountBlockedException();
        }
        if (account.isChangePasswordRequired()) {
            return challengeIssuer.issue(account.getIdentityId(), now);
        }

        AuthTokens tokens = tokenIssuanceService.issue(account, now);
        account.recordSuccessfulLogin(now);
        accountRepository.save(account);
        return new AuthenticatedLoginResult(tokens);
    }

    @Transactional
    public AuthTokens completeRequiredPasswordChange(String rawToken, String newPassword) {
        Instant now = Instant.now(clock);
        String tokenHash = tokenHashService.hash(rawToken);
        Long userId = challengeRepository.findUserIdByTokenHash(tokenHash)
                .orElseThrow(this::invalidChallenge);
        AuthenticationAccount account = accountRepository.findByIdentityIdForUpdate(userId)
                .orElseThrow(() -> new AuthenticationAccountNotFoundException("Authentication account not found."));
        PasswordChangeChallenge challenge = challengeRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(this::invalidChallenge);

        if (!challenge.getUserId().equals(account.getIdentityId()) || !challenge.isValid(now)) {
            throw invalidChallenge();
        }
        account.assertCanAttemptLogin();
        if (!account.isChangePasswordRequired()) {
            throw new PasswordChangeNotRequiredException();
        }

        PasswordRecoveryRequest request = null;
        if (!account.isPendingFirstAccess()) {
            request = recoveryRequestRepository
                    .findOpenByUserIdAndMethodForUpdate(
                            account.getIdentityId(),
                            PasswordRecoveryRequestMethod.ADMIN_TEMPORARY_PASSWORD,
                            now
                    )
                    .orElseThrow(() -> new PasswordRecoveryRequestNotFoundException(
                            "Open temporary password recovery request not found."
                    ));
        }

        account.changePassword(passwordHasher.hash(newPassword), now);
        if (request != null) {
            request.resolve(now);
        }
        challenge.markAsUsed(now);
        credentialRevocationService.revokeAllForUser(account.getIdentityId(), now);

        AuthTokens tokens = tokenIssuanceService.issue(account, now);
        account.recordSuccessfulLogin(now);
        accountRepository.save(account);
        if (request != null) {
            recoveryRequestRepository.save(request);
        }
        challengeRepository.save(challenge);
        return tokens;
    }

    @Transactional(readOnly = true)
    public MeResult getCurrentSession(Long userId, Long sessionId, Instant accessTokenExpiresAt) {
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(sessionId, "sessionId cannot be null");
        Objects.requireNonNull(accessTokenExpiresAt, "accessTokenExpiresAt cannot be null");
        Instant now = Instant.now(clock);
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Session not found"));
        if (!session.getUserId().equals(userId)) {
            throw new SessionUserMismatchException("Session does not belong to this user");
        }
        return new MeResult(
                userId,
                session.getId(),
                session.isValid(now),
                Math.max(Duration.between(now, accessTokenExpiresAt).getSeconds(), 0)
        );
    }

    @Transactional
    public void logout(Long userId, Long sessionId) {
        Session session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
        if (!session.getUserId().equals(userId)) {
            throw new SessionUserMismatchException(
                    "Session does not belong to the authenticated user."
            );
        }
        session.logout(Instant.now(clock));
        sessionRepository.save(session);
        refreshTokenRepository.revokeActiveBySessionId(sessionId);
    }

    private PasswordChangeChallengeInvalidException invalidChallenge() {
        return new PasswordChangeChallengeInvalidException(
                "Password change challenge is invalid or expired."
        );
    }
}
