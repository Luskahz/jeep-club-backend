package com.jeepclub.backend.vehicles.core.application.exceptions;

public class VehicleFieldRequiredException extends RuntimeException {
    public VehicleFieldRequiredException(String message) {
        super(message);
    }
}
