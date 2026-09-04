package com.jeepclub.backend.iam.identity.api.module.spi;

import com.jeepclub.backend.iam.identity.api.module.UserAuthenticationTokens;

import java.time.Instant;

public interface UserAuthenticationProvisioningPort {
    UserAuthenticationTokens provisionAndAuthenticate(
            Long userId,
            String rawPassword,
            Instant now
    );

    void provisionPermanent(Long userId, String rawPassword, Instant now);

    void provisionPendingFirstAccess(Long userId, String rawPassword, Instant now);
}
