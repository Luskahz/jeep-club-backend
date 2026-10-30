package com.jeepclub.backend.dependents.core.repository;

import com.jeepclub.backend.dependents.core.domain.enums.DependentStatus;

/** Internal scalar snapshot. Never serialize as an HTTP response or log a key. */
public record DependentPhotoReference(Long ownerId,DependentStatus status,String storageKey) {
    @Override public String toString() {return "DependentPhotoReference[private]";}
}
