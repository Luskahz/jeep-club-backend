package com.jeepclub.backend.authentication.core.application.services;

import com.jeepclub.backend.authentication.core.application.exceptions.login.PasswordChangeChallengeInvalidException;
import com.jeepclub.backend.authentication.core.application.exceptions.login.PasswordRecoveryRequestNotFoundException;
import com.jeepclub.backend.authentication.core.application.exceptions.user.UserCpfNotFoundException;
import com.jeepclub.backend.authentication.core.application.exceptions.user.UserIdNotFoundException;
import com.jeepclub.backend.authentication.core.application.exceptions.user.UserInvalidPasswordException;
import com.jeepclub.backend.authentication.core.application.exceptions.user.UserPasswordChangeNotRequiredException;
import com.jeepclub.backend.authentication.core.application.results.AuthTokens;
import com.jeepclub.backend.authentication.core.application.results.login.AuthenticatedLoginResult;
import com.jeepclub.backend.authentication.core.application.results.login.LoginResult;
import com.jeepclub.backend.authentication.core.application.results.login.PasswordChangeRequiredLoginResult;
import com.jeepclub.backend.authentication.core.domain.enums.PasswordRecoveryRequestMethod;
import com.jeepclub.backend.authentication.core.domain.model.*;
import com.jeepclub.backend.authentication.core.port.ApplicationTimeProperties;
import com.jeepclub.backend.authentication.core.port.JwtService;
import com.jeepclub.backend.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.authentication.core.port.RefreshTokenGenerator;
import com.jeepclub.backend.authentication.core.port.RefreshTokenHashService;
import com.jeepclub.backend.authentication.core.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordChangeChallengeRepository passwordChangeChallengeRepository;
    private final PasswordHasher passwordHasher;
    private final RefreshTokenHashService tokenHashService;
    private final RefreshTokenGenerator tokenGenerator;
    private final JwtService jwtService;
    private final ApplicationTimeProperties authTimeProperties;
    private final Clock clock;
    private PasswordRecoveryRequestRepository passwordRecoveryRequestRepository;

    @Transactional
    public LoginResult login(String cpf, String senha) {
        Instant now = Instant.now(clock);

        User user = userRepository.findByCpf(cpf)
                .orElseThrow(() -> new UserCpfNotFoundException("CPF not found."));

        if (!passwordHasher.matches(senha, user.getPasswordHash())) {
            user.registerFailedLogin();
            userRepository.save(user);
            throw new UserInvalidPasswordException(user.getId());
        }

        user.assertCanAttemptLogin();

        if (user.isChangePasswordRequired()) {
            PasswordChangeRequiredLoginResult result =
                    createPasswordChangeRequiredResult(user, now);

            userRepository.save(user);

            return result;
        }

        AuthTokens tokens = authenticateUser(user, now);

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

        String tokenHash = tokenHashService.hash(passwordChangeToken);

        PasswordChangeChallenge challenge = passwordChangeChallengeRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() -> new PasswordChangeChallengeInvalidException(
                        "Password change challenge is invalid or expired."
                ));

        if (!challenge.isValid(now)) {
            throw new PasswordChangeChallengeInvalidException(
                    "Password change challenge is invalid or expired."
            );
        }

        User user = userRepository.findById(challenge.getUserId())
                .orElseThrow(() -> new UserIdNotFoundException("User not found."));

        user.assertCanAttemptLogin();

        if (!user.isChangePasswordRequired()) {
            throw new UserPasswordChangeNotRequiredException();
        }

        PasswordRecoveryRequest recoveryRequest = passwordRecoveryRequestRepository
                .findOpenByUserIdAndMethod(
                        user.getId(),
                        PasswordRecoveryRequestMethod.ADMIN_TEMPORARY_PASSWORD,
                        now
                )
                .orElseThrow(() -> new PasswordRecoveryRequestNotFoundException(
                        "Open temporary password recovery request not found."
                ));

        String newPasswordHash = passwordHasher.hash(newPassword);

        user.changePassword(newPasswordHash, now);
        recoveryRequest.resolve(now);
        challenge.markAsUsed(now);

        AuthTokens tokens = authenticateUser(user, now);

        user.recordSuccessfulLogin(now);

        userRepository.save(user);
        passwordRecoveryRequestRepository.save(recoveryRequest);
        passwordChangeChallengeRepository.save(challenge);

        return tokens;
    }
    private PasswordChangeRequiredLoginResult createPasswordChangeRequiredResult(
            User user,
            Instant now
    ) {
        String rawToken = tokenGenerator.generate();
        String tokenHash = tokenHashService.hash(rawToken);

        Instant expiresAt = now.plus(authTimeProperties.passwordChangeChallengeTtl());

        PasswordChangeChallenge challenge = PasswordChangeChallenge.create(
                user.getId(),
                tokenHash,
                now,
                expiresAt
        );

        passwordChangeChallengeRepository.save(challenge);

        return new PasswordChangeRequiredLoginResult(
                rawToken,
                expiresAt
        );
    }

    private AuthTokens authenticateUser(
            User user,
            Instant now
    ) {
        Session session = sessionRepository.findActiveByUserId(user.getId())
                .filter(existing -> existing.isValid(now))
                .orElseGet(() -> sessionRepository.save(
                        Session.create(user.getId(), authTimeProperties.sessionTtl())
                ));

        String rawToken = tokenGenerator.generate();
        String tokenHash = tokenHashService.hash(rawToken);

        RefreshToken refreshToken = RefreshToken.create(
                session,
                tokenHash,
                authTimeProperties.refreshTokenTtl()
        );

        refreshTokenRepository.save(refreshToken, session);

        IssuedAccessToken issuedAccessToken = jwtService.generateAccessToken(user, session);

        long expiresInSeconds = Math.max(
                Duration.between(now, issuedAccessToken.expiresAt()).getSeconds(),
                0
        );

        return new AuthTokens(
                rawToken,
                issuedAccessToken.token(),
                expiresInSeconds
        );
    }
}