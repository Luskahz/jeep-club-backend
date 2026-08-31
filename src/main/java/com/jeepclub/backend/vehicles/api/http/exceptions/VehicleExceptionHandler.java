package com.jeepclub.backend.vehicles.api.http.exceptions;

import com.jeepclub.backend.vehicles.core.application.exceptions.UserNotFoundException;
import com.jeepclub.backend.vehicles.core.application.exceptions.VehicleIdNotFoundException;
import com.jeepclub.backend.vehicles.core.application.exceptions.VehiclePlateAlreadyExistsException;
import com.jeepclub.backend.vehicles.core.application.exceptions.VehicleRenavamAlreadyExistsException;
import com.jeepclub.backend.vehicles.core.domain.exception.VehicleAlreadyDeletedException;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.platform.web.exception.ApiExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.jeepclub.backend.vehicles")
public class VehicleExceptionHandler extends ApiExceptionHandler {

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

    @ExceptionHandler(VehicleIdNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleVehicleIdNotFoundException(VehicleIdNotFoundException exception) {
        return buildErrorResponse(
                "VEHICLE_ID_NOT_FOUND",
                exception.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotFoundException(UserNotFoundException exception) {
        return buildErrorResponse(
                "USER_NOT_FOUND",
                exception.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(VehicleAlreadyDeletedException.class)
    public ResponseEntity<ApiErrorResponse> handleVehicleAlreadyDeleted(
            VehicleAlreadyDeletedException exception
    ) {
        return buildErrorResponse(
                "VEHICLE_ALREADY_DELETED",
                exception.getMessage(),
                HttpStatus.CONFLICT
        );
    }
}
