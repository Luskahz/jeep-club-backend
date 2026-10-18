package com.jeepclub.backend.memberships.api.http.exception;
import com.jeepclub.backend.memberships.core.domain.exception.MemberRegistryException;
import com.jeepclub.backend.platform.web.exception.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
@RestControllerAdvice(basePackages="com.jeepclub.backend.memberships.api.http.controller.admin") @Order(-1)
public class MemberRegistryExceptionHandler extends ApiExceptionHandler {
    @ExceptionHandler(MemberRegistryException.class)
    public ResponseEntity<ApiErrorResponse> handle(MemberRegistryException ex) {
        var status=switch(ex.getKind()) { case INVALID -> HttpStatus.BAD_REQUEST; case NOT_FOUND -> HttpStatus.NOT_FOUND; case CONFLICT,DISABLED -> HttpStatus.CONFLICT; };
        return buildErrorResponse("MEMBER_REGISTRY_"+ex.getKind(),ex.getMessage(),status);
    }
    @ExceptionHandler(org.springframework.dao.ConcurrencyFailureException.class)
    public ResponseEntity<ApiErrorResponse> concurrency(org.springframework.dao.ConcurrencyFailureException ex) {
        return buildErrorResponse("MEMBER_REGISTRY_CONFLICT","Member registry operation conflicts with current state.",HttpStatus.CONFLICT);
    }
}
