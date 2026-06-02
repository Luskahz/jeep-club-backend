package com.jeepclub.backend.vehicles.core.application.exceptions;

public class VehicleIdNotFoundException extends RuntimeException {
    public VehicleIdNotFoundException(String message) {
        super(message);
    }
}
