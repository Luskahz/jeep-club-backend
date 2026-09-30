package com.jeepclub.backend.vehicles.core.application.query;

import com.jeepclub.backend.vehicles.api.module.EventVehicleQuery;
import com.jeepclub.backend.vehicles.core.repository.VehicleRepository;
import com.jeepclub.backend.vehicles.core.repository.VehicleHistoricalPresentationRepository;
import com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service @RequiredArgsConstructor
public class EventVehicleQueryService implements EventVehicleQuery {
    private final VehicleHistoricalPresentationRepository history;
    @Transactional(readOnly=true)
    public java.util.List<Details> findDetailsBatch(java.util.Collection<Long> ids) {
        if(ids.isEmpty())return java.util.List.of();
        if(ids.size()>500)throw new IllegalArgumentException("Maximum batch: 500");
        return vehicles.findAllByIds(ids).stream().filter(v->v.getStatus()==VehicleStatus.ACTIVE)
            .map(v->new Details(v.getId(),v.getOwnerId(),v.getNickname(),v.getPlate(),v.getBrand(),v.getModel(),v.getSeatingCapacity())).toList();
    }

    @Transactional(readOnly=true)
    public java.util.List<Details> findPresentationDetailsBatch(java.util.Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return java.util.List.of();
        if (ids.size() > 500) throw new IllegalArgumentException("Maximum batch: 500");
        var current = vehicles.findAllByIds(ids).stream()
            .map(v -> new Details(v.getId(), v.getOwnerId(), v.getNickname(), v.getPlate(),
                v.getBrand(), v.getModel(), v.getSeatingCapacity())).toList();
        var missing = new java.util.HashSet<>(ids);
        current.forEach(v -> missing.remove(v.id()));
        if (missing.isEmpty()) return current;
        var result = new java.util.ArrayList<>(current);
        result.addAll(history.findByOriginalIds(missing));
        return java.util.List.copyOf(result);
    }

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
