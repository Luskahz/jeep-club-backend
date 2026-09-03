package com.jeepclub.backend.authentication.infra.integration.identity;

import com.jeepclub.backend.authentication.core.application.service.internal.CredentialRevocationService;
import com.jeepclub.backend.authentication.core.domain.model.AuthenticationAccount;
import com.jeepclub.backend.authentication.core.repository.AuthenticationAccountRepository;
import com.jeepclub.backend.identity.api.module.exception.IdentityNotFoundException;
import com.jeepclub.backend.identity.api.module.spi.IdentityAuthenticationAdministrationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class IdentityAuthenticationAdministrationAdapter
        implements IdentityAuthenticationAdministrationPort {

    private final AuthenticationAccountRepository accountRepository;
    private final CredentialRevocationService credentialRevocationService;

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
                .orElseThrow(() -> new IdentityNotFoundException(identityId));
    }
}
