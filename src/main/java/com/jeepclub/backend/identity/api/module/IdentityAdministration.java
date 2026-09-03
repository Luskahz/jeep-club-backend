package com.jeepclub.backend.identity.api.module;

import java.time.Instant;

public interface IdentityAdministration {

    IdentityDetails disable(Long identityId, Instant now);

    IdentityDetails enable(Long identityId, Instant now);
}
