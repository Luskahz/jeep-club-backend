package com.jeepclub.backend.vehicles.core.application.exceptions;

public class VehiclePlateAlreadyExistsException extends RuntimeException {
    public VehiclePlateAlreadyExistsException(String message) {
        super(message);
    }
}
