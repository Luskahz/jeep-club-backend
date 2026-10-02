package com.jeepclub.backend.publications.api.http.exception;
import com.jeepclub.backend.platform.web.exception.*;
import com.jeepclub.backend.publications.api.http.controller.admin.AdminEventController;
import com.jeepclub.backend.publications.api.http.controller.member.EventController;
import com.jeepclub.backend.publications.core.application.exception.EventOperationException;
import com.jeepclub.backend.billing.api.module.EventBillingException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.dao.DataAccessException;
@RestControllerAdvice(assignableTypes={AdminEventController.class, com.jeepclub.backend.publications.api.http.controller.admin.AdminEventReportController.class, EventController.class})
public class EventExceptionHandler extends ApiExceptionHandler {
    @ExceptionHandler(com.jeepclub.backend.shared.storage.exception.InvalidStorageKeyException.class)
    public ResponseEntity<ApiErrorResponse> imageInvalid(Exception e) {
        return buildErrorResponse("INVALID_IMAGE", "Image key is invalid.", HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(com.jeepclub.backend.shared.storage.exception.StorageObjectNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> imageMissing(Exception e) {
        return buildErrorResponse("IMAGE_NOT_FOUND", "Image was not found.", HttpStatus.NOT_FOUND);
    }
    @ExceptionHandler(EventOperationException.class)
    public ResponseEntity<ApiErrorResponse> operation(EventOperationException e) {
        var status = e.getCode().endsWith("NOT_FOUND") ? HttpStatus.NOT_FOUND : e.getCode().equals("EVENT_HEALTH_ACCESS_NOT_ALLOWED") ? HttpStatus.FORBIDDEN : HttpStatus.CONFLICT;
        return buildErrorResponse(e.getCode(), e.getMessage(), status);
    }
    @ExceptionHandler(EventBillingException.class)
    public ResponseEntity<ApiErrorResponse> billing(EventBillingException e) {
        if (e.reason() == EventBillingException.Reason.CHARGE_NOT_FOUND)
            return buildErrorResponse("EVENT_CHARGE_NOT_FOUND", "Event charge was not found.", HttpStatus.NOT_FOUND);
        return buildErrorResponse("EVENT_CHARGE_INVALID", "Event charge is unavailable or incompatible.", HttpStatus.CONFLICT);
    }
    @ExceptionHandler({IllegalArgumentException.class, NullPointerException.class, tools.jackson.core.JacksonException.class})
    public ResponseEntity<ApiErrorResponse> invalid(Exception e) {
        return buildErrorResponse("EVENT_INVALID_REQUEST", "Event fields are invalid.", HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> state(Exception e) {
        return buildErrorResponse("EVENT_INVALID_STATE", "Operation is not allowed in the current state.", HttpStatus.CONFLICT);
    }
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiErrorResponse> conflict(Exception e) {
        return buildErrorResponse("EVENT_CONFLICT", "Concurrent change or persistent conflict; refresh the event.", HttpStatus.CONFLICT);
    }
}
