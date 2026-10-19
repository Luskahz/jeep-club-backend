package com.jeepclub.backend.iam.identity.api.module;

import java.time.Instant;

public interface UserAdministration {

    UserDetails disable(Long userId, Instant now);

    UserDetails enable(Long userId, Instant now);

    UserDetails archive(Long userId, Long actorId, Instant now);

    UserDetails unarchive(Long userId, Long actorId, Instant now);

    void delete(Long userId, Long actorId, Instant now);
}
