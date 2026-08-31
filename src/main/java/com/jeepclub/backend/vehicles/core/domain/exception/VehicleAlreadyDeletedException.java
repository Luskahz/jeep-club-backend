package com.jeepclub.backend.vehicles.core.domain.exception;

public class VehicleAlreadyDeletedException extends RuntimeException {

    public VehicleAlreadyDeletedException(Long vehicleId) {
        super(
                "Vehicle with id "
                        + vehicleId
                        + " is already deleted."
        );
    }
}
