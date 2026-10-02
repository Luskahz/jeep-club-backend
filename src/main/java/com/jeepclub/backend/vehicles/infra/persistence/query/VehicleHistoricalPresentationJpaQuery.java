package com.jeepclub.backend.vehicles.infra.persistence.query;

import com.jeepclub.backend.vehicles.api.module.EventVehicleQuery;
import com.jeepclub.backend.vehicles.core.repository.VehicleHistoricalPresentationRepository;
import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository @RequiredArgsConstructor
public class VehicleHistoricalPresentationJpaQuery implements VehicleHistoricalPresentationRepository {
    private final EntityManager em;

    public List<EventVehicleQuery.Details> findByOriginalIds(Collection<Long> ids) {
        if (ids.isEmpty()) return List.of();
        return em.createQuery("select h.vehicleId,h.ownerId,h.nickname,h.plate,h.brand,h.model,h.seatingCapacity from VehicleHistoryEntity h where h.vehicleId in :ids", Object[].class)
            .setParameter("ids", ids).getResultList().stream()
            .map(row -> new EventVehicleQuery.Details((Long) row[0], (Long) row[1], (String) row[2],
                (String) row[3], (String) row[4], (String) row[5], (Integer) row[6])).toList();
    }
}
