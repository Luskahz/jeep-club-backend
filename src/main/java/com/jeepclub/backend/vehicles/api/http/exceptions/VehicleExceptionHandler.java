package com.jeepclub.backend.vehicles.api.http.exceptions;

import com.jeepclub.backend.vehicles.api.http.dto.edit.MalformedEditPayloadException;
import com.jeepclub.backend.vehicles.core.application.exceptions.UserNotActiveException;
import com.jeepclub.backend.vehicles.core.application.exceptions.UserNotFoundException;
import com.jeepclub.backend.vehicles.core.application.exceptions.VehicleFieldRequiredException;
import com.jeepclub.backend.vehicles.core.application.exceptions.VehicleIdNotFoundException;
import com.jeepclub.backend.vehicles.core.application.exceptions.VehiclePlateAlreadyExistsException;
import com.jeepclub.backend.vehicles.core.application.exceptions.VehicleRenavamAlreadyExistsException;
import com.jeepclub.backend.vehicles.core.domain.exception.VehicleAlreadyDeletedException;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.platform.web.exception.ApiExceptionHandler;
import com.jeepclub.backend.platform.web.exception.ValidationFieldErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;

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

    @ExceptionHandler(UserNotActiveException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotActiveException(UserNotActiveException exception) {
        return buildErrorResponse(
                "USER_NOT_ACTIVE",
                exception.getMessage(),
                HttpStatus.CONFLICT
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

    @ExceptionHandler(VehicleFieldRequiredException.class)
    public ResponseEntity<ApiErrorResponse> handleVehicleFieldRequired(
            VehicleFieldRequiredException exception
    ) {
        return buildErrorResponse(
                "VEHICLE_FIELD_REQUIRED",
                exception.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(MalformedEditPayloadException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedEditPayload(
            MalformedEditPayloadException exception
    ) {
        return buildErrorResponse(
                "INVALID_REQUEST_BODY",
                exception.getMessage(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception
    ) {
        ResponseEntity<ApiErrorResponse> response = buildErrorResponse(
                "VALIDATION_ERROR",
                "The request contains invalid fields.",
                HttpStatus.BAD_REQUEST
        );
        response.getBody().setErrors(
                exception.getConstraintViolations().stream()
                        .map(VehicleExceptionHandler::toFieldErrorResponse)
                        .toList()
        );
        return response;
    }

    private static ValidationFieldErrorResponse toFieldErrorResponse(
            ConstraintViolation<?> violation
    ) {
        String field = violation.getPropertyPath().toString();
        String constraintCode = violation.getConstraintDescriptor()
                .getAnnotation()
                .annotationType()
                .getSimpleName();
        String responseCode = constraintCode
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toUpperCase(Locale.ROOT);

        return new ValidationFieldErrorResponse(
                field,
                responseCode,
                violation.getMessage()
        );
    }
}
