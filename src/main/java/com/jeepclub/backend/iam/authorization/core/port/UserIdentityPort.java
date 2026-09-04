package com.jeepclub.backend.iam.authorization.core.port;

public interface UserIdentityPort {
    boolean existsById(Long userId);
}
