package com.jeepclub.backend.platform.logging;

import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RequestContextEnrichmentFilterTest {

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void enrichesDownstreamLogsFromAuthenticatedPrincipalAndRemovesIdentityAfterward() throws Exception {
        var principal = new UserPrincipal(
                42L, 7L, "Lucas\nAlves", Instant.parse("2026-09-13T12:00:00Z")
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
        var request = new MockHttpServletRequest("GET", "/vehicles");

        new RequestContextEnrichmentFilter().doFilter(
                request,
                new MockHttpServletResponse(),
                (req, res) -> {
                    assertThat(MDC.get(HttpLoggingContext.USER_ID)).isEqualTo("42");
                    assertThat(MDC.get(HttpLoggingContext.USER_NAME)).isEqualTo("Lucas_Alves");
                }
        );

        assertThat(MDC.get(HttpLoggingContext.USER_ID)).isNull();
        assertThat(MDC.get(HttpLoggingContext.USER_NAME)).isNull();
        assertThat(request.getAttribute(HttpLoggingContext.USER_ID_ATTRIBUTE)).isEqualTo("42");
        assertThat(request.getAttribute(HttpLoggingContext.USER_NAME_ATTRIBUTE)).isEqualTo("Lucas_Alves");
    }

    @Test
    void anonymousRequestDoesNotInheritIdentity() throws Exception {
        MDC.put(HttpLoggingContext.USER_ID, "stale");
        MDC.remove(HttpLoggingContext.USER_ID);

        new RequestContextEnrichmentFilter().doFilter(
                new MockHttpServletRequest("GET", "/public"),
                new MockHttpServletResponse(),
                (req, res) -> assertThat(MDC.get(HttpLoggingContext.USER_ID)).isNull()
        );
    }

    @Test
    void legacyPrincipalKeepsCanonicalUserIdWithoutInventingUserName() throws Exception {
        var principal = new UserPrincipal(
                42L, 7L, null, Instant.parse("2026-09-13T12:00:00Z")
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
        var request = new MockHttpServletRequest("GET", "/authorization/me");

        new RequestContextEnrichmentFilter().doFilter(
                request,
                new MockHttpServletResponse(),
                (req, res) -> {
                    assertThat(MDC.get(HttpLoggingContext.USER_ID)).isEqualTo("42");
                    assertThat(MDC.get(HttpLoggingContext.USER_NAME)).isNull();
                }
        );

        assertThat(request.getAttribute(HttpLoggingContext.USER_ID_ATTRIBUTE)).isEqualTo("42");
        assertThat(request.getAttribute(HttpLoggingContext.USER_NAME_ATTRIBUTE)).isNull();
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }
}
