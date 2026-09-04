package com.jeepclub.backend.authentication.core.application.service.passwordrecovery;

import com.jeepclub.backend.authentication.core.application.exceptions.login.PasswordRecoveryRequestNotFoundException;
import com.jeepclub.backend.authentication.core.application.exceptions.account.AuthenticationAccountNotFoundException;
import com.jeepclub.backend.authentication.core.application.result.IssuedPasswordResetToken;
import com.jeepclub.backend.authentication.core.application.result.IssuedTemporaryPassword;
import com.jeepclub.backend.authentication.core.application.result.PasswordResetLinkAdminResult;
import com.jeepclub.backend.authentication.core.application.result.TemporaryPasswordAdminResult;
import com.jeepclub.backend.authentication.core.application.result.admin.recovery.AdminPasswordRecoveryRequestResult;
import com.jeepclub.backend.authentication.core.application.service.internal.CredentialRevocationService;
import com.jeepclub.backend.authentication.core.application.service.internal.PasswordRecoveryRequestManager;
import com.jeepclub.backend.authentication.core.application.service.internal.PasswordResetTokenIssuer;
import com.jeepclub.backend.authentication.core.application.service.internal.TemporaryPasswordIssuer;
import com.jeepclub.backend.authentication.core.domain.model.PasswordRecoveryRequest;
import com.jeepclub.backend.authentication.core.domain.model.AuthenticationAccount;
import com.jeepclub.backend.authentication.core.repository.PasswordRecoveryRequestRepository;
import com.jeepclub.backend.authentication.core.repository.AuthenticationAccountRepository;
import com.jeepclub.backend.identity.api.module.UserQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminPasswordRecoveryService {

    private final AuthenticationAccountRepository accountRepository;
    private final UserQuery identityQuery;
    private final PasswordRecoveryRequestRepository requestRepository;
    private final PasswordRecoveryRequestManager requestManager;
    private final TemporaryPasswordIssuer passwordIssuer;
    private final CredentialRevocationService revocationService;
    private final PasswordResetTokenIssuer tokenIssuer;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<AdminPasswordRecoveryRequestResult> findAll() {
        return AdminPasswordRecoveryRequestResult.from(requestRepository.findAll());
    }

    @Transactional(readOnly = true)
    public AdminPasswordRecoveryRequestResult findById(Long id) {
        return AdminPasswordRecoveryRequestResult.from(find(id));
    }

    @Transactional(readOnly = true)
    public List<AdminPasswordRecoveryRequestResult> findByUserId(Long userId) {
        if (!identityQuery.existsById(userId)) {
            throw new AuthenticationAccountNotFoundException(userId);
        }
        return AdminPasswordRecoveryRequestResult.from(requestRepository.findByUserId(userId));
    }

    @Transactional
    public AdminPasswordRecoveryRequestResult cancel(Long id) {
        PasswordRecoveryRequest request = requestRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new PasswordRecoveryRequestNotFoundException(id));
        request.cancel(Instant.now(clock));
        return AdminPasswordRecoveryRequestResult.from(requestRepository.save(request));
    }

    @Transactional
    public TemporaryPasswordAdminResult generateTemporaryPassword(Long userId) {
        Instant now = Instant.now(clock);
        AuthenticationAccount account = accountRepository.findByIdentityIdForUpdate(userId)
                .orElseThrow(() -> new AuthenticationAccountNotFoundException("Authentication account target not found."));
        account.assertCanRequestPasswordChange();
        PasswordRecoveryRequest request = requestManager.getOrCreate(account.getIdentityId(), now);
        IssuedTemporaryPassword temporaryPassword = passwordIssuer.issue();
        revocationService.revokeAllForUser(account.getIdentityId(), now);
        request.changeToAdminTemporaryPasswordMethod(now);
        account.changeToTemporaryPassword(temporaryPassword.passwordHash(), now);
        accountRepository.save(account);
        return new TemporaryPasswordAdminResult(
                temporaryPassword.rawPassword(), requestRepository.save(request)
        );
    }

    @Transactional
    public PasswordResetLinkAdminResult generateResetLink(Long userId) {
        Instant now = Instant.now(clock);
        AuthenticationAccount account = accountRepository.findByIdentityIdForUpdate(userId)
                .orElseThrow(() -> new AuthenticationAccountNotFoundException("Authentication account target not found."));
        account.assertCanRequestPasswordChange();
        PasswordRecoveryRequest request = requestManager.getOrCreate(account.getIdentityId(), now);
        IssuedPasswordResetToken token = tokenIssuer.issue();
        request.changeToAdminResetLinkMethod(token.tokenHash(), now);
        PasswordRecoveryRequest saved = requestRepository.save(request);
        return new PasswordResetLinkAdminResult(
                token.resetLink(),
                saved
        );
    }

    private PasswordRecoveryRequest find(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new PasswordRecoveryRequestNotFoundException(id));
    }
}
