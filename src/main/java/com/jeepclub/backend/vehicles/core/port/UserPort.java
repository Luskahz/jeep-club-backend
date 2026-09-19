package com.jeepclub.backend.vehicles.core.port;

public interface UserPort {
    boolean existsById(Long userId);

    boolean existsActiveById(Long userId);
}
