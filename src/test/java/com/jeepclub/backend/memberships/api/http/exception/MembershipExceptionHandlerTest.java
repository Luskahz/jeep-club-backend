package com.jeepclub.backend.memberships.api.http.exception;

import com.jeepclub.backend.memberships.core.application.exception.MembershipApplicantBlockedException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class MembershipExceptionHandlerTest {

    @Test
    void blockedApplicantResponseIsForbiddenAndDoesNotExposeReason() {
        var response = new MembershipExceptionHandler().handleApplicantBlocked(
                new MembershipApplicantBlockedException()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("MEMBERSHIP_APPLICATION_NOT_ALLOWED");
        assertThat(response.getBody().getDetail())
                .isEqualTo("Não é possível realizar uma nova solicitação de associação.")
                .doesNotContain("motivo");
    }
}
