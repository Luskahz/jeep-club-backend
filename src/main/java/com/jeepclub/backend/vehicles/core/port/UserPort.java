package com.jeepclub.backend.vehicles.core.port;

public interface UserPort {
    java.util.Set<Long> findIdsByName(String name);
    java.util.Map<Long, String> findNamesByIds(java.util.Collection<Long> userIds);
    boolean existsById(Long userId);

    boolean existsActiveById(Long userId);
}
