package com.jeepclub.backend.memberships.api.http.exception;

import com.jeepclub.backend.memberships.core.application.exception.*;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.platform.web.exception.ApiExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.jeepclub.backend.memberships")
public class MembershipExceptionHandler extends ApiExceptionHandler {

    @ExceptionHandler(MembershipApplicationNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(MembershipApplicationNotFoundException ex) {
        return buildErrorResponse("MEMBERSHIP_APPLICATION_NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MembershipApplicationAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleAlreadyExists(MembershipApplicationAlreadyExistsException ex) {
        return buildErrorResponse("MEMBERSHIP_APPLICATION_ALREADY_EXISTS", ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MembershipApplicationAlreadyProcessedException.class)
    public ResponseEntity<ApiErrorResponse> handleAlreadyProcessed(MembershipApplicationAlreadyProcessedException ex) {
        return buildErrorResponse("MEMBERSHIP_APPLICATION_ALREADY_PROCESSED", ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MembershipApplicantBlockedException.class)
    public ResponseEntity<ApiErrorResponse> handleApplicantBlocked(MembershipApplicantBlockedException ex) {
        return buildErrorResponse("MEMBERSHIP_APPLICATION_NOT_ALLOWED", ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MembershipApplicantAlreadyBlockedException.class)
    public ResponseEntity<ApiErrorResponse> handleApplicantAlreadyBlocked(
            MembershipApplicantAlreadyBlockedException ex
    ) {
        return buildErrorResponse("MEMBERSHIP_APPLICANT_ALREADY_BLOCKED", ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MembershipApplicantBlockNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleApplicantBlockNotFound(
            MembershipApplicantBlockNotFoundException ex
    ) {
        return buildErrorResponse("MEMBERSHIP_APPLICANT_BLOCK_NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MemberActivationTokenNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleTokenNotFound(MemberActivationTokenNotFoundException ex) {
        return buildErrorResponse("ACTIVATION_TOKEN_NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MemberActivationTokenExpiredException.class)
    public ResponseEntity<ApiErrorResponse> handleTokenExpired(MemberActivationTokenExpiredException ex) {
        return buildErrorResponse("ACTIVATION_TOKEN_EXPIRED", ex.getMessage(), HttpStatus.GONE);
    }

    @ExceptionHandler(MemberActivationTokenAlreadyUsedException.class)
    public ResponseEntity<ApiErrorResponse> handleTokenAlreadyUsed(MemberActivationTokenAlreadyUsedException ex) {
        return buildErrorResponse("ACTIVATION_TOKEN_ALREADY_USED", ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(IllegalStateException ex) {
        return buildErrorResponse("INVALID_OPERATION", ex.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(MembershipCpfAlreadyRegisteredException.class)
    public ResponseEntity<ApiErrorResponse> handleCpfAlreadyRegistered(
            MembershipCpfAlreadyRegisteredException ex
    ) {
        return buildErrorResponse(
                "MEMBERSHIP_CPF_ALREADY_REGISTERED",
                ex.getMessage(),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(MembershipEmailAlreadyInUseException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailAlreadyInUse(
            MembershipEmailAlreadyInUseException ex
    ) {
        return buildErrorResponse(
                "MEMBERSHIP_EMAIL_ALREADY_IN_USE",
                ex.getMessage(),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(MembershipEmailAlreadyRegisteredException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailAlreadyRegistered(
            MembershipEmailAlreadyRegisteredException ex
    ) {
        return buildErrorResponse(
                "MEMBERSHIP_EMAIL_ALREADY_REGISTERED",
                ex.getMessage(),
                HttpStatus.CONFLICT
        );
    }
}
