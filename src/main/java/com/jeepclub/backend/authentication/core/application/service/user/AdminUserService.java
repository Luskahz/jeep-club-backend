package com.jeepclub.backend.authentication.core.application.service.user;

import com.jeepclub.backend.authentication.api.http.dto.admin.user.AdminUserFilterDTO;
import com.jeepclub.backend.authentication.core.application.exceptions.user.UserIdNotFoundException;
import com.jeepclub.backend.authentication.core.application.query.user.AdminUserField;
import com.jeepclub.backend.authentication.core.application.query.user.AdminUserFilter;
import com.jeepclub.backend.authentication.core.application.result.admin.user.AdminUserResult;
import com.jeepclub.backend.authentication.core.application.service.internal.CredentialRevocationService;
import com.jeepclub.backend.authentication.core.domain.model.AuthenticationAccount;
import com.jeepclub.backend.authentication.core.repository.AdminUserQueryRepository;
import com.jeepclub.backend.authentication.core.repository.AuthenticationAccountRepository;
import com.jeepclub.backend.identity.api.module.IdentityAdministration;
import com.jeepclub.backend.identity.api.module.IdentityDetails;
import com.jeepclub.backend.identity.api.module.IdentityQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final AuthenticationAccountRepository accountRepository;
    private final IdentityQuery identityQuery;
    private final IdentityAdministration identityAdministration;
    private final CredentialRevocationService credentialRevocationService;
    private final AdminUserQueryRepository adminUserQueryRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Page<AdminUserResult> findAll(
            AdminUserFilter filter,
            Set<AdminUserField> fields,
            Pageable pageable
    ) {
        return adminUserQueryRepository.findAll(
                filter,
                fields,
                pageable
        );
    }

    @Transactional(readOnly = true)
    public AdminUserResult findById(Long userId) {
        IdentityDetails identity = findIdentityById(userId);
        AuthenticationAccount account = findAccountById(userId);
        return AdminUserResult.from(identity, account);
    }

    @Transactional
    public AdminUserResult disable(Long userId) {
        Instant now = Instant.now(clock);
        findIdentityById(userId);
        AuthenticationAccount account = findAccountByIdForUpdate(userId);
        IdentityDetails identity = identityAdministration.disable(userId, now);
        account.disableAccess(now);
        credentialRevocationService.revokeAllForUser(userId, now);
        return AdminUserResult.from(identity, accountRepository.save(account));
    }

    @Transactional
    public AdminUserResult enable(Long userId) {
        Instant now = Instant.now(clock);
        findIdentityById(userId);
        AuthenticationAccount account = findAccountByIdForUpdate(userId);
        IdentityDetails identity = identityAdministration.enable(userId, now);
        account.enableAccess(now);
        return AdminUserResult.from(identity, accountRepository.save(account));
    }

    private IdentityDetails findIdentityById(Long userId) {
        return identityQuery.findById(userId)
                .orElseThrow(
                        () -> new UserIdNotFoundException(
                                userId
                        )
                );
    }

    private AuthenticationAccount findAccountById(Long userId) {
        return accountRepository.findByIdentityId(userId)
                .orElseThrow(() -> new UserIdNotFoundException(userId));
    }

    private AuthenticationAccount findAccountByIdForUpdate(Long userId) {
        return accountRepository.findByIdentityIdForUpdate(userId)
                .orElseThrow(
                        () -> new UserIdNotFoundException(
                                userId
                        )
                );
    }
}
