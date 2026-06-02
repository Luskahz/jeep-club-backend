package com.jeepclub.backend.vehicles.core.application.exceptions;

public class VehicleRenavamAlreadyExistsException extends RuntimeException {
    public VehicleRenavamAlreadyExistsException(String message) {
        super(message);
    }
}
