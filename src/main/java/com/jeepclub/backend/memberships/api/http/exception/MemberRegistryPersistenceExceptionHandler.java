package com.jeepclub.backend.memberships.api.http.exception;
import com.jeepclub.backend.platform.web.exception.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
@RestControllerAdvice(assignableTypes={
    com.jeepclub.backend.memberships.api.http.controller.admin.AdminMemberController.class,
    com.jeepclub.backend.memberships.api.http.controller.admin.AdminMemberImportController.class
}) @Order(-1)
public class MemberRegistryPersistenceExceptionHandler extends ApiExceptionHandler {
    @ExceptionHandler({org.springframework.dao.ConcurrencyFailureException.class,org.springframework.dao.DataIntegrityViolationException.class})
    public ResponseEntity<ApiErrorResponse> conflict(RuntimeException failure) {
        return buildErrorResponse("MEMBER_REGISTRY_CONFLICT","Member registry operation conflicts with current state.",HttpStatus.CONFLICT);
    }
}
