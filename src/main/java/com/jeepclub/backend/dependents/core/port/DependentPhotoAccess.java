package com.jeepclub.backend.dependents.core.port;

/** Nonlocking reads; joins the photo authorization snapshot. */
public interface DependentPhotoAccess {
    boolean isActiveActor(Long actorId);
    boolean canReadAsAdmin(Long actorId);
}
