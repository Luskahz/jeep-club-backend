package com.jeepclub.backend.vehicles.api.module;
import java.util.Optional;
/** Parent lock for Event writes; ordinary presentation keeps its existing query contract. */
public interface VehicleLifecycleLock { Optional<EventVehicleQuery.VehicleCapacity> activeById(Long id); }
