package com.jeepclub.backend.identity.api.module.spi;

import java.time.Instant;

/**
 * Authentication-side effects required by the administrative Identity lifecycle.
 */
public interface IdentityAuthenticationAdministrationPort {

    void disableAuthentication(Long identityId, Instant now);

    void enableAuthentication(Long identityId, Instant now);
}
