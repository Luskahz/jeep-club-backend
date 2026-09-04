package com.jeepclub.backend.iam.identity.api.module;

import java.time.Instant;

public interface UserAdministration {

    UserDetails disable(Long userId, Instant now);

    UserDetails enable(Long userId, Instant now);
}
