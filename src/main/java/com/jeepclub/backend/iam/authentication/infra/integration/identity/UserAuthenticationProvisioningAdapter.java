package com.jeepclub.backend.iam.authentication.infra.integration.identity;

import com.jeepclub.backend.iam.authentication.core.application.exceptions.account.AuthenticationAccountConflictException;
import com.jeepclub.backend.iam.authentication.core.application.result.AuthTokens;
import com.jeepclub.backend.iam.authentication.core.application.service.internal.TokenIssuanceService;
import com.jeepclub.backend.iam.authentication.core.domain.model.AuthenticationAccount;
import com.jeepclub.backend.iam.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.iam.authentication.core.repository.AuthenticationAccountRepository;
import com.jeepclub.backend.iam.identity.api.module.UserAuthenticationTokens;
import com.jeepclub.backend.iam.identity.api.module.exception.UserRegistrationConflictException;
import com.jeepclub.backend.iam.identity.api.module.spi.UserAuthenticationProvisioningPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class UserAuthenticationProvisioningAdapter implements UserAuthenticationProvisioningPort {
    private final AuthenticationAccountRepository accountRepository;
    private final PasswordHasher passwordHasher;
    private final TokenIssuanceService tokenIssuanceService;

    @Override
    public UserAuthenticationTokens provisionAndAuthenticate(
            Long userId,
            String rawPassword,
            Instant now
    ) {
        AuthenticationAccount account = create(userId, rawPassword, now, false);
        AuthTokens tokens = tokenIssuanceService.issue(account, now);
        account.recordSuccessfulLogin(now);
        accountRepository.save(account);
        return new UserAuthenticationTokens(
                tokens.refreshToken(),
                tokens.accessToken(),
                tokens.expiresInSeconds()
        );
    }

    @Override
    public void provisionPermanent(Long userId, String rawPassword, Instant now) {
        create(userId, rawPassword, now, false);
    }

    @Override
    public void provisionPendingFirstAccess(Long userId, String rawPassword, Instant now) {
        create(userId, rawPassword, now, true);
    }

    private AuthenticationAccount create(
            Long userId,
            String rawPassword,
            Instant now,
            boolean pendingFirstAccess
    ) {
        String passwordHash = passwordHasher.hash(rawPassword);
        AuthenticationAccount account = pendingFirstAccess
                ? AuthenticationAccount.createPendingFirstAccess(userId, passwordHash, now)
                : AuthenticationAccount.create(userId, passwordHash, now);
        try {
            return accountRepository.create(account);
        } catch (AuthenticationAccountConflictException exception) {
            throw new UserRegistrationConflictException(exception);
        }
    }
}
