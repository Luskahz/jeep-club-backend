package com.jeepclub.backend.health.api.http.security;

import com.jeepclub.backend.health.core.application.audit.MedicalProfileAuditEvent;
import com.jeepclub.backend.health.core.application.audit.MedicalProfileAuditOperation;
import com.jeepclub.backend.health.core.application.audit.MedicalProfileAuditOutcome;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.port.MedicalProfileAuditTrail;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MedicalProfileAuthorizationAuditFilterTest {

    private static final Instant NOW = Instant.parse("2026-09-07T16:00:00Z");

    @Mock
    private MedicalProfileAuditTrail auditTrail;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deniedAdministrativeAuthorizationIsAuditedWithoutRequestBody() throws Exception {
        authenticate(99L);
        MedicalProfileAuthorizationAuditFilter filter = filter();
        MockHttpServletRequest request = new MockHttpServletRequest(
                "PUT",
                "/admin/medical-profiles/dependents/11"
        );
        request.setServletPath("/admin/medical-profiles/dependents/11");
        request.setContent("ALERGIA-SECRETA".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(
                request,
                response,
                (ignoredRequest, chainResponse) ->
                        ((MockHttpServletResponse) chainResponse).setStatus(403)
        );

        verify(auditTrail).record(new MedicalProfileAuditEvent(
                99L,
                MedicalProfileOwnerType.DEPENDENT,
                11L,
                MedicalProfileAuditOperation.UPDATE,
                MedicalProfileAuditOutcome.DENIED,
                NOW
        ));
    }

    @Test
    void successfulAdministrativeRequestIsNotDuplicatedByAuthorizationFilter()
            throws Exception {
        authenticate(99L);
        MedicalProfileAuthorizationAuditFilter filter = filter();
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/admin/medical-profiles/users/7"
        );
        request.setServletPath("/admin/medical-profiles/users/7");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(
                request,
                response,
                (ignoredRequest, chainResponse) ->
                        ((MockHttpServletResponse) chainResponse).setStatus(200)
        );

        verify(auditTrail, never()).record(org.mockito.ArgumentMatchers.any());
    }

    private MedicalProfileAuthorizationAuditFilter filter() {
        return new MedicalProfileAuthorizationAuditFilter(
                Optional.of(auditTrail),
                Optional.of(Clock.fixed(NOW, ZoneOffset.UTC))
        );
    }

    private void authenticate(Long userId) {
        UserPrincipal principal = new UserPrincipal(
                userId,
                1L,
                NOW.plusSeconds(3600)
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of()
                )
        );
    }
}
