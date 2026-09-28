package com.jeepclub.backend.vehicles.api.module;

import java.util.Optional;

public interface EventVehicleQuery {
    record Details(Long id, Long ownerId, String nickname, String plate, String brand, String model, int seatingCapacity) {}
    java.util.List<Details> findDetailsBatch(java.util.Collection<Long> ids);

    Optional<VehicleCapacity> findActive(Long vehicleId);
    java.util.List<VehicleCapacity> findActiveBatch(java.util.Collection<Long> vehicleIds);
    record VehicleCapacity(Long id, Long ownerId, int seatingCapacity) {}
}
