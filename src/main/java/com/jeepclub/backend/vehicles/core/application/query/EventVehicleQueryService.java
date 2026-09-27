package com.jeepclub.backend.vehicles.core.application.query;

import com.jeepclub.backend.vehicles.api.module.EventVehicleQuery;
import com.jeepclub.backend.vehicles.core.repository.VehicleRepository;
import com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service @RequiredArgsConstructor
public class EventVehicleQueryService implements EventVehicleQuery {
    private final VehicleRepository vehicles;
    @Transactional(readOnly = true)
    public java.util.List<VehicleCapacity> findActiveBatch(java.util.Collection<Long> ids) {
        return vehicles.findAllByIds(ids).stream().filter(v -> v.getStatus() == VehicleStatus.ACTIVE)
            .map(v -> new VehicleCapacity(v.getId(), v.getOwnerId(), v.getSeatingCapacity())).toList();
    }
    @Transactional(readOnly = true)
    public Optional<VehicleCapacity> findActive(Long id) {
        return vehicles.findById(id).filter(v -> v.getStatus() == VehicleStatus.ACTIVE)
            .map(v -> new VehicleCapacity(v.getId(), v.getOwnerId(), v.getSeatingCapacity()));
    }
}
