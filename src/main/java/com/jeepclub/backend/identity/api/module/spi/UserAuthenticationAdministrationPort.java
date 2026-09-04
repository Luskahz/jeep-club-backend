package com.jeepclub.backend.identity.api.module.spi;

import java.time.Instant;

/**
 * Authentication-side effects required by the administrative User lifecycle.
 */
public interface UserAuthenticationAdministrationPort {

    void disableAuthentication(Long identityId, Instant now);

    void enableAuthentication(Long identityId, Instant now);
}
