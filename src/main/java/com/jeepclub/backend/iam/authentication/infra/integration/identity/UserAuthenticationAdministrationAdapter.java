package com.jeepclub.backend.iam.authentication.infra.integration.identity;

import com.jeepclub.backend.iam.authentication.core.repository.AccountActivationInvitationRepository;
import com.jeepclub.backend.iam.authentication.core.repository.PasswordRecoveryRequestRepository;
import com.jeepclub.backend.iam.authentication.core.application.service.internal.CredentialRevocationService;
import com.jeepclub.backend.iam.authentication.core.application.exceptions.account.AuthenticationAccountNotFoundException;
import com.jeepclub.backend.iam.authentication.core.domain.model.AuthenticationAccount;
import com.jeepclub.backend.iam.authentication.core.repository.AuthenticationAccountRepository;
import com.jeepclub.backend.iam.identity.api.module.spi.UserAuthenticationAdministrationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class UserAuthenticationAdministrationAdapter
        implements UserAuthenticationAdministrationPort {

    private final AuthenticationAccountRepository accountRepository;
    private final CredentialRevocationService credentialRevocationService;
    private final AccountActivationInvitationRepository invitations;
    private final PasswordRecoveryRequestRepository recoveries;

    @Override
    public void identityChanged(Long identityId, boolean cpfChanged, boolean emailChanged, Instant now) {
        accountRepository.findByIdentityIdForUpdate(identityId)
                .orElseThrow(com.jeepclub.backend.iam.identity.api.module.exception.UserIdentityChangeConflictException::new);
        credentialRevocationService.revokeAllForUser(identityId, now);
        if (emailChanged) {
            invitations.findById(identityId).filter(invitation -> invitation.usedAt() == null).ifPresent(invitation -> {
                invitation.invalidate(now);
                invitations.save(invitation);
            });
            recoveries.findOpenEmailTokensByUserIdForUpdate(identityId)
                    .forEach(request -> { request.cancel(now); recoveries.save(request); });
        }
    }

    @Override
    public void disableAuthentication(Long identityId, Instant now) {
        AuthenticationAccount account = findForUpdate(identityId);
        account.disableAccess(now);
        accountRepository.save(account);
        credentialRevocationService.revokeAllForUser(identityId, now);
    }

    @Override
    public void enableAuthentication(Long identityId, Instant now) {
        AuthenticationAccount account = findForUpdate(identityId);
        account.enableAccess(now);
        accountRepository.save(account);
    }

    private AuthenticationAccount findForUpdate(Long identityId) {
        return accountRepository.findByIdentityIdForUpdate(identityId)
                .orElseThrow(() -> new AuthenticationAccountNotFoundException(identityId));
    }
}
