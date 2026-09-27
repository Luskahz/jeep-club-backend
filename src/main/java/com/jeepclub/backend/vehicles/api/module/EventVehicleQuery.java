package com.jeepclub.backend.vehicles.api.module;

import java.util.Optional;

public interface EventVehicleQuery {
    Optional<VehicleCapacity> findActive(Long vehicleId);
    java.util.List<VehicleCapacity> findActiveBatch(java.util.Collection<Long> vehicleIds);
    record VehicleCapacity(Long id, Long ownerId, int seatingCapacity) {}
}
