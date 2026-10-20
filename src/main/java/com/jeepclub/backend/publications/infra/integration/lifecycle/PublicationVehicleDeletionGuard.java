package com.jeepclub.backend.publications.infra.integration.lifecycle;
import com.jeepclub.backend.vehicles.api.module.spi.VehicleDeletionGuard;
import com.jeepclub.backend.publications.infra.persistence.jpa.EventRegistrationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor
public class PublicationVehicleDeletionGuard implements VehicleDeletionGuard {
    private final EventRegistrationJpaRepository references;
    @Override public boolean hasRetainedReferences(Long id) { return references.lockVehicleAllocation(id).isPresent() || references.lockVehicleOccupant(id).isPresent() || references.lockVehicleGuest(id).isPresent() || references.lockVehicleOffer(id).isPresent(); }
}
