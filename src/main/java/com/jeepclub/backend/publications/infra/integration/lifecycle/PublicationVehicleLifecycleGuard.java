package com.jeepclub.backend.publications.infra.integration.lifecycle;

import com.jeepclub.backend.vehicles.api.module.spi.VehicleLifecycleGuard;
import com.jeepclub.backend.publications.infra.persistence.adapter.EventLifecycleParticipation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component @RequiredArgsConstructor
class PublicationVehicleLifecycleGuard implements VehicleLifecycleGuard {
    private final EventLifecycleParticipation participation;
    public boolean hasOperationalParticipation(Long id, Instant now) { return participation.vehicle(id,now); }
}
