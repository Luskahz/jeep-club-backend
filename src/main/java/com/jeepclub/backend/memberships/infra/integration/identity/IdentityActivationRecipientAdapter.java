package com.jeepclub.backend.memberships.infra.integration.identity;

import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import com.jeepclub.backend.memberships.core.port.ActivationRecipient;
import com.jeepclub.backend.memberships.core.port.ActivationRecipientPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class IdentityActivationRecipientAdapter implements ActivationRecipientPort {
    private final UserQuery userQuery;

    @Override
    public Optional<ActivationRecipient> findByIdentityId(Long identityId) {
        return userQuery.findById(identityId)
                .map(user -> new ActivationRecipient(user.name(), user.email()));
    }
}
