package com.jeepclub.backend.vehicles.api.module.spi;

import java.time.Instant;

/** Called while holding the Vehicle lifecycle lock. Must not acquire Event locks. */
public interface VehicleLifecycleGuard {
    boolean hasOperationalParticipation(Long vehicleId, Instant now);
}
