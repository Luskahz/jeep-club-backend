package com.jeepclub.backend.vehicles.core.domain.exception;

public class VehicleLifecycleConflictException extends RuntimeException {
    public VehicleLifecycleConflictException(String message) { super(message); }
}
