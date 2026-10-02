package com.jeepclub.backend.tools.core.port;

public interface ToolOwnerQuery {
    boolean existsById(Long userId);

    boolean isAdministrativelyActive(Long userId);
}
