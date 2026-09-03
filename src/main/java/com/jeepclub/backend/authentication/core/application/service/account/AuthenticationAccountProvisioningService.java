package com.jeepclub.backend.authentication.core.application.service.account;

import com.jeepclub.backend.authentication.core.domain.model.AuthenticationAccount;
import com.jeepclub.backend.authentication.core.repository.AuthenticationAccountRepository;
import com.jeepclub.backend.identity.api.module.IdentityRegistration;
import com.jeepclub.backend.identity.api.module.IdentityRegistrationData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationAccountProvisioningService {

    private final IdentityRegistration identityRegistration;
    private final AuthenticationAccountRepository accountRepository;

    @Transactional
    public Long provision(
            IdentityRegistrationData identityData,
            String passwordHash
    ) {
        return provision(identityData, passwordHash, false);
    }

    @Transactional
    public Long provisionPendingFirstAccess(
            IdentityRegistrationData identityData,
            String passwordHash
    ) {
        return provision(identityData, passwordHash, true);
    }

    private Long provision(
            IdentityRegistrationData identityData,
            String passwordHash,
            boolean pendingFirstAccess
    ) {
        Long identityId = identityRegistration.create(identityData);
        AuthenticationAccount account = pendingFirstAccess
                ? AuthenticationAccount.createPendingFirstAccess(
                        identityId,
                        passwordHash,
                        identityData.now()
                )
                : AuthenticationAccount.create(
                        identityId,
                        passwordHash,
                        identityData.now()
                );

        return accountRepository.create(account).getIdentityId();
    }
}
