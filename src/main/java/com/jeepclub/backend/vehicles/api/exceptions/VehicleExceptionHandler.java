package com.jeepclub.backend.vehicles.api.exceptions;

import com.jeepclub.backend.vehicles.core.application.exceptions.VehiclePlateAlreadyExistsException;
import com.jeepclub.backend.vehicles.core.application.exceptions.VehicleRenavamAlreadyExistsException;
import com.jeepclub.backend.infra.web.exception.ApiErrorResponse;
import com.jeepclub.backend.shared.exception.BuilderExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.jeepclub.backend.vehicles")
public class VehicleExceptionHandler extends BuilderExceptionHandler {

    @ExceptionHandler(VehiclePlateAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleVehiclePlateAlreadyExists(VehiclePlateAlreadyExistsException exception) {
        return buildErrorResponse(
                "VEHICLE_PLATE_ALREADY_EXISTS",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(VehicleRenavamAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleVehicleRenavamAlreadyExists(VehicleRenavamAlreadyExistsException exception) {
        return buildErrorResponse(
                "VEHICLE_RENAVAM_ALREADY_EXISTS",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }
}