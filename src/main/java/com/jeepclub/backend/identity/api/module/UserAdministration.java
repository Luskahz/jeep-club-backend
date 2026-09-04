package com.jeepclub.backend.identity.api.module;

import java.time.Instant;

public interface UserAdministration {

    UserDetails disable(Long userId, Instant now);

    UserDetails enable(Long userId, Instant now);
}
