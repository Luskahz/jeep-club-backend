package com.jeepclub.backend.identity.api.module;

import java.time.Instant;

public interface UserAdministration {

    UserDetails disable(Long identityId, Instant now);

    UserDetails enable(Long identityId, Instant now);
}
