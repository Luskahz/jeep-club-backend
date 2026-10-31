package com.jeepclub.backend.dependents.core.port;

/** Authentication-owned validation in the consuming photo snapshot. */
public interface DependentPhotoSessionAccess {
    void validate(Long actorId,Long sessionId);
}
