package com.jeepclub.backend.authentication.core.application.service.session;

import com.jeepclub.backend.authentication.core.application.exceptions.login.InvalidCredentialsException;
import com.jeepclub.backend.authentication.core.application.exceptions.login.PasswordChangeChallengeInvalidException;
import com.jeepclub.backend.authentication.core.application.exceptions.login.PasswordRecoveryRequestNotFoundException;
import com.jeepclub.backend.authentication.core.application.exceptions.session.SessionNotFoundException;
import com.jeepclub.backend.authentication.core.application.exceptions.session.SessionUserMismatchException;
import com.jeepclub.backend.authentication.core.application.exceptions.user.UserIdNotFoundException;
import com.jeepclub.backend.authentication.core.application.exceptions.user.UserPasswordChangeNotRequiredException;
import com.jeepclub.backend.authentication.core.application.result.AuthTokens;
import com.jeepclub.backend.authentication.core.application.result.MeResult;
import com.jeepclub.backend.authentication.core.application.result.login.AuthenticatedLoginResult;
import com.jeepclub.backend.authentication.core.application.result.login.LoginResult;
import com.jeepclub.backend.authentication.core.application.service.internal.CredentialRevocationService;
import com.jeepclub.backend.authentication.core.application.service.internal.PasswordChangeChallengeIssuer;
import com.jeepclub.backend.authentication.core.application.service.internal.TokenIssuanceService;
import com.jeepclub.backend.authentication.core.domain.enums.AccountStatus;
import com.jeepclub.backend.authentication.core.domain.enums.PasswordRecoveryRequestMethod;
import com.jeepclub.backend.authentication.core.domain.model.PasswordChangeChallenge;
import com.jeepclub.backend.authentication.core.domain.model.PasswordRecoveryRequest;
import com.jeepclub.backend.authentication.core.domain.model.Session;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.authentication.core.port.RefreshTokenHashService;
import com.jeepclub.backend.authentication.core.repository.PasswordChangeChallengeRepository;
import com.jeepclub.backend.authentication.core.repository.PasswordRecoveryRequestRepository;
import com.jeepclub.backend.authentication.core.repository.RefreshTokenRepository;
import com.jeepclub.backend.authentication.core.repository.SessionRepository;
import com.jeepclub.backend.authentication.core.repository.UserRepository;
import com.jeepclub.backend.identity.api.module.IdentityDetails;
import com.jeepclub.backend.identity.api.module.IdentityQuery;
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

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordChangeChallengeRepository challengeRepository;
    private final PasswordRecoveryRequestRepository recoveryRequestRepository;
    private final PasswordHasher passwordHasher;
    private final RefreshTokenHashService tokenHashService;
    private final CredentialRevocationService credentialRevocationService;
    private final PasswordChangeChallengeIssuer challengeIssuer;
    private final TokenIssuanceService tokenIssuanceService;
    private final IdentityQuery identityQuery;
    private final Clock clock;

    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public LoginResult login(String cpf, String password) {
        Instant now = Instant.now(clock);
        User user = userRepository.findByCpfForUpdate(cpf)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordHasher.matches(password, user.getPasswordHash())) {
            user.registerFailedLogin();
            userRepository.save(user);
            throw new InvalidCredentialsException();
        }

        user.assertCanAttemptLogin();
        if (user.isChangePasswordRequired()) {
            return challengeIssuer.issue(user.getId(), now);
        }

        AuthTokens tokens = tokenIssuanceService.issue(user, now);
        user.recordSuccessfulLogin(now);
        userRepository.save(user);
        return new AuthenticatedLoginResult(tokens);
    }

    @Transactional
    public AuthTokens completeRequiredPasswordChange(String rawToken, String newPassword) {
        Instant now = Instant.now(clock);
        String tokenHash = tokenHashService.hash(rawToken);
        Long userId = challengeRepository.findUserIdByTokenHash(tokenHash)
                .orElseThrow(this::invalidChallenge);
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserIdNotFoundException("User not found."));
        PasswordChangeChallenge challenge = challengeRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(this::invalidChallenge);

        if (!challenge.getUserId().equals(user.getId()) || !challenge.isValid(now)) {
            throw invalidChallenge();
        }
        user.assertCanAttemptLogin();
        if (!user.isChangePasswordRequired()) {
            throw new UserPasswordChangeNotRequiredException();
        }

        PasswordRecoveryRequest request = null;
        if (!user.isPendingFirstAccess()) {
            request = recoveryRequestRepository
                    .findOpenByUserIdAndMethodForUpdate(
                            user.getId(),
                            PasswordRecoveryRequestMethod.ADMIN_TEMPORARY_PASSWORD,
                            now
                    )
                    .orElseThrow(() -> new PasswordRecoveryRequestNotFoundException(
                            "Open temporary password recovery request not found."
                    ));
        }

        user.changePassword(passwordHasher.hash(newPassword), now);
        if (request != null) {
            request.resolve(now);
        }
        challenge.markAsUsed(now);
        credentialRevocationService.revokeAllForUser(user.getId(), now);

        AuthTokens tokens = tokenIssuanceService.issue(user, now);
        user.recordSuccessfulLogin(now);
        userRepository.save(user);
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
        IdentityDetails identity = identityQuery.findById(userId)
                .orElseThrow(() -> new UserIdNotFoundException("User not found"));
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Session not found"));
        if (!session.getUserId().equals(identity.id())) {
            throw new SessionUserMismatchException("Session does not belong to this user");
        }
        return new MeResult(
                identity.id(),
                identity.name(),
                identity.birthDate(),
                identity.email(),
                identity.cpf(),
                identity.rg(),
                identity.phoneNumber(),
                identity.profilePhotoUrl(),
                identity.administrativelyActive()
                        ? AccountStatus.ACTIVE
                        : AccountStatus.DISABLED,
                identity.createdAt(),
                identity.updatedAt(),
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
