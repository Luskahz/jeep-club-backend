package com.jeepclub.backend.iam.identity.api.module.spi;

import java.time.Instant;

/**
 * Authentication-side effects required by the administrative User lifecycle.
 */
public interface UserAuthenticationAdministrationPort {

    void disableAuthentication(Long identityId, Instant now);

    void enableAuthentication(Long identityId, Instant now);

    /** Called after Identity has locked and flushed new contact data, in the same transaction. */
    void identityChanged(Long identityId, boolean cpfChanged, boolean emailChanged, Instant now);
}
