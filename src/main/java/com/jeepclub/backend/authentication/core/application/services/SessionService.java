package com.jeepclub.backend.authentication.core.application.services;

import com.jeepclub.backend.authentication.core.application.exceptions.login.InvalidCredentialsException;
import com.jeepclub.backend.authentication.core.application.exceptions.login.PasswordChangeChallengeInvalidException;
import com.jeepclub.backend.authentication.core.application.exceptions.login.PasswordRecoveryRequestNotFoundException;
import com.jeepclub.backend.authentication.core.application.exceptions.session.SessionNotFoundException;
import com.jeepclub.backend.authentication.core.application.exceptions.session.SessionUserMismatchException;
import com.jeepclub.backend.authentication.core.application.exceptions.user.UserIdNotFoundException;
import com.jeepclub.backend.authentication.core.application.exceptions.user.UserPasswordChangeNotRequiredException;
import com.jeepclub.backend.authentication.core.application.results.AuthTokens;
import com.jeepclub.backend.authentication.core.application.results.MeResult;
import com.jeepclub.backend.authentication.core.application.results.login.AuthenticatedLoginResult;
import com.jeepclub.backend.authentication.core.application.results.login.LoginResult;
import com.jeepclub.backend.authentication.core.application.results.login.PasswordChangeRequiredLoginResult;
import com.jeepclub.backend.authentication.core.domain.enums.PasswordRecoveryRequestMethod;
import com.jeepclub.backend.authentication.core.domain.model.IssuedAccessToken;
import com.jeepclub.backend.authentication.core.domain.model.PasswordChangeChallenge;
import com.jeepclub.backend.authentication.core.domain.model.PasswordRecoveryRequest;
import com.jeepclub.backend.authentication.core.domain.model.RefreshToken;
import com.jeepclub.backend.authentication.core.domain.model.Session;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.port.ApplicationTimeProperties;
import com.jeepclub.backend.authentication.core.port.JwtService;
import com.jeepclub.backend.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.authentication.core.port.RefreshTokenGenerator;
import com.jeepclub.backend.authentication.core.port.RefreshTokenHashService;
import com.jeepclub.backend.authentication.core.repository.PasswordChangeChallengeRepository;
import com.jeepclub.backend.authentication.core.repository.PasswordRecoveryRequestRepository;
import com.jeepclub.backend.authentication.core.repository.RefreshTokenRepository;
import com.jeepclub.backend.authentication.core.repository.SessionRepository;
import com.jeepclub.backend.authentication.core.repository.UserRepository;
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
    private final PasswordChangeChallengeRepository passwordChangeChallengeRepository;
    private final PasswordRecoveryRequestRepository passwordRecoveryRequestRepository;
    private final PasswordHasher passwordHasher;
    private final RefreshTokenHashService tokenHashService;
    private final RefreshTokenGenerator tokenGenerator;
    private final JwtService jwtService;
    private final ApplicationTimeProperties authTimeProperties;
    private final Clock clock;

    @Transactional(
            noRollbackFor = InvalidCredentialsException.class
    )
    public LoginResult login(
            String cpf,
            String senha
    ) {
        Instant now = Instant.now(clock);

        User user = userRepository
                .findByCpfForUpdate(cpf)
                .orElseThrow(
                        InvalidCredentialsException::new
                );

        if (
                !passwordHasher.matches(
                        senha,
                        user.getPasswordHash()
                )
        ) {
            user.registerFailedLogin();
            userRepository.save(user);

            throw new InvalidCredentialsException();
        }

        user.assertCanAttemptLogin();

        if (user.isChangePasswordRequired()) {
            PasswordChangeRequiredLoginResult result =
                    createPasswordChangeRequiredResult(
                            user,
                            now
                    );

            userRepository.save(user);

            return result;
        }

        AuthTokens tokens =
                authenticateUser(user, now);

        user.recordSuccessfulLogin(now);
        userRepository.save(user);

        return new AuthenticatedLoginResult(tokens);
    }

    @Transactional
    public AuthTokens completeRequiredPasswordChange(
            String passwordChangeToken,
            String newPassword
    ) {
        Instant now = Instant.now(clock);

        String tokenHash =
                tokenHashService.hash(passwordChangeToken);

        Long userId = passwordChangeChallengeRepository
                .findUserIdByTokenHash(tokenHash)
                .orElseThrow(
                        () -> invalidPasswordChangeChallenge()
                );

        User user = userRepository
                .findByIdForUpdate(userId)
                .orElseThrow(
                        () -> new UserIdNotFoundException(
                                "User not found."
                        )
                );

        PasswordChangeChallenge challenge =
                passwordChangeChallengeRepository
                        .findByTokenHashForUpdate(tokenHash)
                        .orElseThrow(
                                () -> invalidPasswordChangeChallenge()
                        );

        if (
                !challenge.getUserId().equals(user.getId())
                        || !challenge.isValid(now)
        ) {
            throw invalidPasswordChangeChallenge();
        }

        user.assertCanAttemptLogin();

        if (!user.isChangePasswordRequired()) {
            throw new UserPasswordChangeNotRequiredException();
        }

        PasswordRecoveryRequest recoveryRequest =
                passwordRecoveryRequestRepository
                        .findOpenByUserIdAndMethodForUpdate(
                                user.getId(),
                                PasswordRecoveryRequestMethod
                                        .ADMIN_TEMPORARY_PASSWORD,
                                now
                        )
                        .orElseThrow(
                                () -> new PasswordRecoveryRequestNotFoundException(
                                        "Open temporary password recovery request not found."
                                )
                        );

        String newPasswordHash =
                passwordHasher.hash(newPassword);

        user.changePassword(
                newPasswordHash,
                now
        );

        recoveryRequest.resolve(now);
        challenge.markAsUsed(now);

        AuthTokens tokens =
                authenticateUser(user, now);

        user.recordSuccessfulLogin(now);

        userRepository.save(user);
        passwordRecoveryRequestRepository.save(
                recoveryRequest
        );
        passwordChangeChallengeRepository.save(
                challenge
        );

        return tokens;
    }

    @Transactional
    public AuthTokens authenticateRegisteredUser(
            User user
    ) {
        Instant now = Instant.now(clock);

        User lockedUser = userRepository
                .findByIdForUpdate(user.getId())
                .orElseThrow(
                        () -> new UserIdNotFoundException(
                                "Registered user not found."
                        )
                );

        lockedUser.assertCanAuthenticate();

        AuthTokens tokens =
                authenticateUser(lockedUser, now);

        lockedUser.recordSuccessfulLogin(now);
        userRepository.save(lockedUser);

        return tokens;
    }

    private PasswordChangeRequiredLoginResult
    createPasswordChangeRequiredResult(
            User user,
            Instant now
    ) {
        passwordChangeChallengeRepository
                .invalidateActiveByUserId(
                        user.getId(),
                        now
                );

        String rawToken =
                tokenGenerator.generate();

        String tokenHash =
                tokenHashService.hash(rawToken);

        Instant expiresAt = now.plus(
                authTimeProperties
                        .passwordChangeChallengeTtl()
        );

        PasswordChangeChallenge challenge =
                PasswordChangeChallenge.create(
                        user.getId(),
                        tokenHash,
                        now,
                        expiresAt
                );

        passwordChangeChallengeRepository.save(
                challenge
        );

        return new PasswordChangeRequiredLoginResult(
                rawToken,
                expiresAt
        );
    }

    private AuthTokens authenticateUser(
            User user,
            Instant now
    ) {
        Session session = sessionRepository
                .findActiveByUserIdForUpdate(user.getId())
                .filter(existing ->
                        existing.isValid(now)
                )
                .orElseGet(() ->
                        sessionRepository.save(
                                Session.create(
                                        user.getId(),
                                        authTimeProperties.sessionTtl(),
                                        now
                                )
                        )
                );

        String rawToken =
                tokenGenerator.generate();

        String tokenHash =
                tokenHashService.hash(rawToken);

        RefreshToken refreshToken =
                RefreshToken.create(
                        session,
                        tokenHash,
                        authTimeProperties.refreshTokenTtl(),
                        now
                );

        refreshTokenRepository.save(refreshToken);

        IssuedAccessToken issuedAccessToken =
                jwtService.generateAccessToken(
                        user,
                        session
                );

        long expiresInSeconds = Math.max(
                Duration.between(
                        now,
                        issuedAccessToken.expiresAt()
                ).getSeconds(),
                0
        );

        return new AuthTokens(
                rawToken,
                issuedAccessToken.token(),
                expiresInSeconds
        );
    }

    @Transactional(readOnly = true)
    public MeResult me(
            Long userId,
            Long sessionId,
            Instant accessTokenExpiresAt
    ) {
        Objects.requireNonNull(
                userId,
                "userId cannot be null"
        );
        Objects.requireNonNull(
                sessionId,
                "sessionId cannot be null"
        );
        Objects.requireNonNull(
                accessTokenExpiresAt,
                "accessTokenExpiresAt cannot be null"
        );

        Instant now = Instant.now(clock);

        User user = userRepository.findById(userId)
                .orElseThrow(
                        () -> new UserIdNotFoundException(
                                "User not found"
                        )
                );

        Session session =
                sessionRepository.findById(sessionId)
                        .orElseThrow(
                                () -> new SessionNotFoundException(
                                        "Session not found"
                                )
                        );

        if (!session.getUserId().equals(user.getId())) {
            throw new SessionUserMismatchException(
                    "Session does not belong to this user"
            );
        }

        return new MeResult(
                user.getId(),
                user.getName(),
                session.getId(),
                session.isValid(now),
                getAccessTokenRemainingSeconds(
                        now,
                        accessTokenExpiresAt
                )
        );
    }

    private long getAccessTokenRemainingSeconds(
            Instant now,
            Instant accessTokenExpiresAt
    ) {
        return Math.max(
                Duration.between(
                        now,
                        accessTokenExpiresAt
                ).getSeconds(),
                0
        );
    }

    @Transactional
    public void logout(
            Long userId,
            Long sessionId
    ) {
        Instant now = Instant.now(clock);

        Session session = sessionRepository
                .findByIdForUpdate(sessionId)
                .orElseThrow(
                        () -> new SessionNotFoundException(
                                sessionId
                        )
                );

        if (!session.getUserId().equals(userId)) {
            throw new SessionUserMismatchException(
                    "Session does not belong to the authenticated user."
            );
        }

        session.logout(now);
        sessionRepository.save(session);
    }

    private PasswordChangeChallengeInvalidException
    invalidPasswordChangeChallenge() {
        return new PasswordChangeChallengeInvalidException(
                "Password change challenge is invalid or expired."
        );
    }
}