package com.jeepclub.backend.memberships.core.port;

import java.util.Optional;

public interface ActivationRecipientPort {
    /** Called inside a transaction; locks Identity before token/account mutations. */
    Optional<ActivationRecipient> findByIdentityId(Long identityId);
}
