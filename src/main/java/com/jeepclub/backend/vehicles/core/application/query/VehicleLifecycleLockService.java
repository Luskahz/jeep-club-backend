package com.jeepclub.backend.vehicles.core.application.query;
import com.jeepclub.backend.vehicles.api.module.*;
import com.jeepclub.backend.vehicles.core.repository.VehicleRepository;
import com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.Optional;
@Service @RequiredArgsConstructor @Transactional(propagation=Propagation.MANDATORY)
class VehicleLifecycleLockService implements VehicleLifecycleLock {
    private final VehicleRepository vehicles;
    @Override public Optional<EventVehicleQuery.VehicleCapacity> activeById(Long id) {
        return vehicles.findByIdForUpdate(id).filter(v -> v.getStatus()==VehicleStatus.ACTIVE)
                .map(v -> new EventVehicleQuery.VehicleCapacity(v.getId(),v.getOwnerId(),v.getSeatingCapacity()));
    }
}
