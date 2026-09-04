package com.jeepclub.backend.authentication.core.application.service.account;

import com.jeepclub.backend.authentication.core.domain.model.AuthenticationAccount;
import com.jeepclub.backend.authentication.core.repository.AuthenticationAccountRepository;
import com.jeepclub.backend.identity.api.module.UserRegistration;
import com.jeepclub.backend.identity.api.module.UserRegistrationData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationAccountProvisioningService {

    private final UserRegistration identityRegistration;
    private final AuthenticationAccountRepository accountRepository;

    @Transactional
    public Long provision(
            UserRegistrationData identityData,
            String passwordHash
    ) {
        return provision(identityData, passwordHash, false);
    }

    @Transactional
    public Long provisionPendingFirstAccess(
            UserRegistrationData identityData,
            String passwordHash
    ) {
        return provision(identityData, passwordHash, true);
    }

    private Long provision(
            UserRegistrationData identityData,
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
