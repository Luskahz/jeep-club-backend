package com.jeepclub.backend.iam.authentication.core.application.service.passwordrecovery;

import com.jeepclub.backend.iam.authentication.core.application.exceptions.tokenhash.TokenInvalidException;
import com.jeepclub.backend.iam.authentication.core.application.exceptions.tokenhash.TokenNotFoundException;
import com.jeepclub.backend.iam.authentication.core.application.exceptions.account.AuthenticationAccountNotFoundException;
import com.jeepclub.backend.iam.authentication.core.application.result.IssuedPasswordResetToken;
import com.jeepclub.backend.iam.authentication.core.application.result.PublicPasswordRecoveryResult;
import com.jeepclub.backend.iam.authentication.core.application.service.internal.CredentialRevocationService;
import com.jeepclub.backend.iam.authentication.core.application.service.internal.PasswordRecoveryRequestManager;
import com.jeepclub.backend.iam.authentication.core.application.service.internal.PasswordResetTokenIssuer;
import com.jeepclub.backend.iam.authentication.core.domain.model.PasswordRecoveryRequest;
import com.jeepclub.backend.iam.authentication.core.domain.model.AuthenticationAccount;
import com.jeepclub.backend.iam.authentication.core.port.NotificationPort;
import com.jeepclub.backend.iam.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.iam.authentication.core.port.RefreshTokenHashService;
import com.jeepclub.backend.iam.authentication.core.repository.PasswordRecoveryRequestRepository;
import com.jeepclub.backend.iam.authentication.core.repository.AuthenticationAccountRepository;
import com.jeepclub.backend.iam.identity.api.module.UserDetails;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {

    private final AuthenticationAccountRepository accountRepository;
    private final UserQuery identityQuery;
    private final PasswordRecoveryRequestRepository requestRepository;
    private final PasswordRecoveryRequestManager requestManager;
    private final NotificationPort notificationPort;
    private final PasswordResetTokenIssuer tokenIssuer;
    private final RefreshTokenHashService tokenHashService;
    private final PasswordHasher passwordHasher;
    private final CredentialRevocationService revocationService;
    private final Clock clock;

    @Transactional
    public PublicPasswordRecoveryResult request(String cpf) {
        Instant now = Instant.now(clock);
        UserDetails identity = identityQuery.findByCpf(cpf).orElse(null);
        if (identity != null && identity.administrativelyActive()
                && accountRepository.existsByIdentityId(identity.id())) {
            requestManager.getOrCreate(identity.id(), now);
        }
        return requestManager.genericResult(now);
    }

    @Transactional
    public PublicPasswordRecoveryResult sendEmailToken(String cpf) {
        Instant now = Instant.now(clock);
        UserDetails identity = identityQuery.findByCpf(cpf).orElse(null);
        if (identity == null || !identity.administrativelyActive()
                || !accountRepository.existsByIdentityId(identity.id())
                || identity.email() == null || identity.email().isBlank()) {
            return requestManager.genericEmailResult(now);
        }
        PasswordRecoveryRequest request = requestManager.getOrCreate(identity.id(), now);
        IssuedPasswordResetToken token = tokenIssuer.issue();
        request.changeToEmailTokenMethod(token.tokenHash(), now);
        requestRepository.save(request);
        notificationPort.sendPasswordResetLink(
                identity.email(),
                token.resetLink()
        );
        return requestManager.genericEmailResult(now);
    }

    @Transactional
    public void resetPasswordByToken(String rawToken, String newPassword) {
        Instant now = Instant.now(clock);
        String tokenHash = tokenHashService.hash(rawToken);
        Long userId = requestRepository.findUserIdByTokenHash(tokenHash)
                .orElseThrow(() -> new TokenNotFoundException("Token invalid or expired."));
        AuthenticationAccount account = accountRepository.findByIdentityIdForUpdate(userId)
                .orElseThrow(() -> new AuthenticationAccountNotFoundException("Authentication account not found."));
        PasswordRecoveryRequest request = requestRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new TokenNotFoundException("Token invalid or expired."));
        if (!request.getUserId().equals(account.getIdentityId())
                || !request.isTokenBased() || !request.isOpen(now)) {
            throw new TokenInvalidException("Token invalid or expired.");
        }
        account.assertCanRequestPasswordChange();
        revocationService.revokeAllForUser(account.getIdentityId(), now);
        account.changePassword(passwordHasher.hash(newPassword), now);
        request.resolve(now);
        accountRepository.save(account);
        requestRepository.save(request);
    }
}
