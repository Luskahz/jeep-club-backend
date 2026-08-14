package com.jeepclub.backend.authorization.core.port;

public interface UserIdentityPort {
    boolean existsById(Long userId);
}
