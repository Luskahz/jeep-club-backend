package com.jeepclub.backend.vehicles.core.repository;

import com.jeepclub.backend.vehicles.api.module.EventVehicleQuery;
import java.util.Collection;
import java.util.List;

public interface VehicleHistoricalPresentationRepository {
    List<EventVehicleQuery.Details> findByOriginalIds(Collection<Long> ids);
}
