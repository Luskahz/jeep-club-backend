package com.jeepclub.backend.memberships.infra.integration.identity;

import com.jeepclub.backend.iam.identity.api.module.UserIdentityLock;
import com.jeepclub.backend.memberships.core.port.ActivationRecipient;
import com.jeepclub.backend.memberships.core.port.ActivationRecipientPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class IdentityActivationRecipientAdapter implements ActivationRecipientPort {
    private final UserIdentityLock identityLock;

    @Override
    public Optional<ActivationRecipient> findByIdentityId(Long identityId) {
        return identityLock.byId(identityId)
                .map(user -> new ActivationRecipient(user.name(), user.email()));
    }
}
