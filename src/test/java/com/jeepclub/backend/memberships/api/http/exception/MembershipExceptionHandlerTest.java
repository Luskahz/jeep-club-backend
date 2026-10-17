package com.jeepclub.backend.memberships.api.http.exception;

import com.jeepclub.backend.memberships.core.application.exception.*;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class MembershipExceptionHandlerTest {

    private MembershipExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new MembershipExceptionHandler();
    }

    @Test
    @DisplayName("Should handle MembershipApplicationNotFoundException with 404")
    void handleNotFound() {
        ResponseEntity<ApiErrorResponse> response = handler.handleNotFound(
                new MembershipApplicationNotFoundException(123L)
        );

        assertResponse(response, HttpStatus.NOT_FOUND, "MEMBERSHIP_APPLICATION_NOT_FOUND");
    }

    @Test
    @DisplayName("Should handle MembershipApplicationAlreadyExistsException with 409")
    void handleAlreadyExists() {
        ResponseEntity<ApiErrorResponse> response = handler.handleAlreadyExists(
                new MembershipApplicationAlreadyExistsException("52998224725")
        );

        assertResponse(response, HttpStatus.CONFLICT, "MEMBERSHIP_APPLICATION_ALREADY_EXISTS");
    }

    @Test
    @DisplayName("Should handle MembershipApplicationAlreadyProcessedException with 409")
    void handleAlreadyProcessed() {
        ResponseEntity<ApiErrorResponse> response = handler.handleAlreadyProcessed(
                new MembershipApplicationAlreadyProcessedException(123L, "APPROVED")
        );

        assertResponse(response, HttpStatus.CONFLICT, "MEMBERSHIP_APPLICATION_ALREADY_PROCESSED");
    }

    @Test
    @DisplayName("Should handle MembershipApplicantBlockedException with 403 and sanitize message")
    void handleApplicantBlocked() {
        ResponseEntity<ApiErrorResponse> response = handler.handleApplicantBlocked(
                new MembershipApplicantBlockedException()
        );

        assertResponse(response, HttpStatus.FORBIDDEN, "MEMBERSHIP_APPLICATION_NOT_ALLOWED");
        assertThat(response.getBody().getDetail())
                .isEqualTo("Não é possível realizar uma nova solicitação de associação.")
                .doesNotContain("motivo");
    }

    @Test
    @DisplayName("Should handle MembershipApplicantAlreadyBlockedException with 409")
    void handleApplicantAlreadyBlocked() {
        ResponseEntity<ApiErrorResponse> response = handler.handleApplicantAlreadyBlocked(
                new MembershipApplicantAlreadyBlockedException("52998224725")
        );

        assertResponse(response, HttpStatus.CONFLICT, "MEMBERSHIP_APPLICANT_ALREADY_BLOCKED");
    }

    @Test
    @DisplayName("Should handle MembershipApplicantBlockNotFoundException with 404")
    void handleApplicantBlockNotFound() {
        ResponseEntity<ApiErrorResponse> response = handler.handleApplicantBlockNotFound(
                new MembershipApplicantBlockNotFoundException("52998224725")
        );

        assertResponse(response, HttpStatus.NOT_FOUND, "MEMBERSHIP_APPLICANT_BLOCK_NOT_FOUND");
    }

    @Test
    @DisplayName("Should handle MemberActivationTokenNotFoundException with 404")
    void handleTokenNotFound() {
        ResponseEntity<ApiErrorResponse> response = handler.handleTokenNotFound(
                new MemberActivationTokenNotFoundException()
        );

        assertResponse(response, HttpStatus.NOT_FOUND, "ACTIVATION_TOKEN_NOT_FOUND");
    }

    @Test
    @DisplayName("Should handle MemberActivationTokenExpiredException with 410")
    void handleTokenExpired() {
        ResponseEntity<ApiErrorResponse> response = handler.handleTokenExpired(
                new MemberActivationTokenExpiredException()
        );

        assertResponse(response, HttpStatus.GONE, "ACTIVATION_TOKEN_EXPIRED");
    }

    @Test
    @DisplayName("Should handle MemberActivationTokenAlreadyUsedException with 409")
    void handleTokenAlreadyUsed() {
        ResponseEntity<ApiErrorResponse> response = handler.handleTokenAlreadyUsed(
                new MemberActivationTokenAlreadyUsedException()
        );

        assertResponse(response, HttpStatus.CONFLICT, "ACTIVATION_TOKEN_ALREADY_USED");
    }

    @Test
    @DisplayName("Should handle IllegalStateException with 422")
    void handleIllegalState() {
        ResponseEntity<ApiErrorResponse> response = handler.handleIllegalState(
                new IllegalStateException("Operação inválida de transição")
        );

        assertResponse(response, HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_OPERATION");
    }

    @Test
    @DisplayName("Should handle MembershipCpfAlreadyRegisteredException with 409")
    void handleCpfAlreadyRegistered() {
        ResponseEntity<ApiErrorResponse> response = handler.handleCpfAlreadyRegistered(
                new MembershipCpfAlreadyRegisteredException("52998224725")
        );

        assertResponse(response, HttpStatus.CONFLICT, "MEMBERSHIP_CPF_ALREADY_REGISTERED");
    }

    @Test
    @DisplayName("Should handle MembershipEmailAlreadyInUseException with 409")
    void handleEmailAlreadyInUse() {
        ResponseEntity<ApiErrorResponse> response = handler.handleEmailAlreadyInUse(
                new MembershipEmailAlreadyInUseException("lucas@example.com")
        );

        assertResponse(response, HttpStatus.CONFLICT, "MEMBERSHIP_EMAIL_ALREADY_IN_USE");
    }

    @Test
    @DisplayName("Should handle MembershipEmailAlreadyRegisteredException with 409")
    void handleEmailAlreadyRegistered() {
        ResponseEntity<ApiErrorResponse> response = handler.handleEmailAlreadyRegistered(
                new MembershipEmailAlreadyRegisteredException("lucas@example.com")
        );

        assertResponse(response, HttpStatus.CONFLICT, "MEMBERSHIP_EMAIL_ALREADY_REGISTERED");
    }

    @Test
    @DisplayName("Should handle MembershipEmailRequiredException with 409")
    void handleEmailRequired() {
        ResponseEntity<ApiErrorResponse> response = handler.handleEmailRequired(
                new MembershipEmailRequiredException()
        );

        assertResponse(response, HttpStatus.CONFLICT, "MEMBERSHIP_EMAIL_REQUIRED");
    }

    @Test
    @DisplayName("Should handle MembershipFirstAccessConflictException with 409")
    void handleFirstAccessConflict() {
        ResponseEntity<ApiErrorResponse> response = handler.handleFirstAccessConflict(
                new MembershipFirstAccessConflictException("Conflito de credencial")
        );

        assertResponse(response, HttpStatus.CONFLICT, "MEMBERSHIP_FIRST_ACCESS_CONFLICT");
    }

    @Test
    @DisplayName("Should handle InvalidMembershipChargeDefinitionException with 422")
    void handleInvalidMembershipChargeDefinition() {
        ResponseEntity<ApiErrorResponse> response = handler.handleInvalidMembershipChargeDefinition(
                new InvalidMembershipChargeDefinitionException()
        );

        assertResponse(response, HttpStatus.UNPROCESSABLE_ENTITY, "MEMBERSHIP_CHARGE_DEFINITION_INVALID");
    }

    @Test
    @DisplayName("Should handle MembershipBillingConfigurationNotFoundException with 404")
    void handleBillingConfigurationNotFound() {
        ResponseEntity<ApiErrorResponse> response = handler.handleBillingConfigurationNotFound(
                new MembershipBillingConfigurationNotFoundException()
        );

        assertResponse(response, HttpStatus.NOT_FOUND, "MEMBERSHIP_BILLING_CONFIGURATION_NOT_FOUND");
    }

    private void assertResponse(ResponseEntity<ApiErrorResponse> response, HttpStatus expectedStatus, String expectedCode) {
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(expectedCode);
        assertThat(response.getBody().getStatus()).isEqualTo(expectedStatus.value());
        assertThat(response.getBody().getDetail()).isNotBlank();
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }
}
