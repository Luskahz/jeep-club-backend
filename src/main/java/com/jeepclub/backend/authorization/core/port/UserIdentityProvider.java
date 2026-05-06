package com.jeepclub.backend.authorization.core.port;

public interface UserIdentityProvider {
    boolean existsById(Long userId);
}
