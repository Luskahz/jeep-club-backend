package com.jeepclub.backend.iam.authentication.core.repository;

import com.jeepclub.backend.iam.authentication.core.domain.model.AccountActivationInvitation;
import java.util.Optional;

public interface AccountActivationInvitationRepository {
    Optional<AccountActivationInvitation> findById(Long identityId);
    Optional<AccountActivationInvitation> findByTokenHash(String tokenHash);
    Optional<AccountActivationInvitation> findByTokenHashForUpdate(String tokenHash);
    void save(AccountActivationInvitation invitation);
}
