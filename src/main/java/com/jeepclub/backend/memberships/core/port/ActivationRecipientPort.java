package com.jeepclub.backend.memberships.core.port;

import java.util.Optional;

public interface ActivationRecipientPort {
    Optional<ActivationRecipient> findByIdentityId(Long identityId);
}
