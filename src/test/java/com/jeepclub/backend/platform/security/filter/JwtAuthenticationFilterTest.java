package com.jeepclub.backend.platform.security.filter;

import com.jeepclub.backend.iam.authentication.core.application.service.security.AccessTokenAuthenticationService;
import com.jeepclub.backend.iam.authentication.core.application.exceptions.session.SessionInvalidException;
import com.jeepclub.backend.platform.security.authorization.UserAuthoritiesProvider;
import com.jeepclub.backend.platform.security.jwt.JwtAuthenticatedUser;
import com.jeepclub.backend.platform.security.jwt.JwtTokenParser;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.web.exception.ApiProblemResponseWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void propagatesJwtUserNameIntoAuthenticatedPrincipalWithoutChangingCanonicalIdentity() throws Exception {
        JwtTokenParser parser = mock(JwtTokenParser.class);
        UserAuthoritiesProvider authoritiesProvider = mock(UserAuthoritiesProvider.class);
        AccessTokenAuthenticationService authenticationService = mock(AccessTokenAuthenticationService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<ApiProblemResponseWriter> problemWriterProvider = mock(ObjectProvider.class);
        Instant expiresAt = Instant.parse("2026-09-13T12:00:00Z");
        when(parser.parseAndValidate("access-token"))
                .thenReturn(new JwtAuthenticatedUser(42L, 7L, "Lucas Alves", expiresAt));
        when(authoritiesProvider.findAuthorityCodesByUserId(42L))
                .thenReturn(List.of("IDENTITY_USER_READ"));
        var filter = new JwtAuthenticationFilter(
                parser,
                authoritiesProvider,
                authenticationService,
                problemWriterProvider
        );
        var request = new MockHttpServletRequest("GET", "/identity/me");
        request.addHeader("Authorization", "Bearer access-token");
        var response = new MockHttpServletResponse();
        var captured = new AtomicReference<UserPrincipal>();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) ->
                captured.set((UserPrincipal) SecurityContextHolder.getContext()
                        .getAuthentication().getPrincipal()));

        assertThat(captured.get().getUserId()).isEqualTo(42L);
        assertThat(captured.get().getSessionId()).isEqualTo(7L);
        assertThat(captured.get().getUserName()).isEqualTo("Lucas Alves");
        assertThat(captured.get().getAccessTokenExpiresAt()).isEqualTo(expiresAt);
        verify(authenticationService).validate(42L, 7L);
    }

    @Test
    void authenticatesLegacyTokenWithoutLookingUpMissingUserName() throws Exception {
        JwtTokenParser parser = mock(JwtTokenParser.class);
        UserAuthoritiesProvider authoritiesProvider = mock(UserAuthoritiesProvider.class);
        AccessTokenAuthenticationService authenticationService = mock(AccessTokenAuthenticationService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<ApiProblemResponseWriter> problemWriterProvider = mock(ObjectProvider.class);
        Instant expiresAt = Instant.parse("2026-09-13T12:00:00Z");
        when(parser.parseAndValidate("legacy-access-token"))
                .thenReturn(new JwtAuthenticatedUser(42L, 7L, null, expiresAt));
        when(authoritiesProvider.findAuthorityCodesByUserId(42L))
                .thenReturn(List.of("IDENTITY_USER_READ"));
        var filter = new JwtAuthenticationFilter(
                parser,
                authoritiesProvider,
                authenticationService,
                problemWriterProvider
        );
        var request = new MockHttpServletRequest("GET", "/identity/me");
        request.addHeader("Authorization", "Bearer legacy-access-token");
        var captured = new AtomicReference<UserPrincipal>();

        filter.doFilter(request, new MockHttpServletResponse(), (ignoredRequest, ignoredResponse) ->
                captured.set((UserPrincipal) SecurityContextHolder.getContext()
                        .getAuthentication().getPrincipal()));

        assertThat(captured.get().getUserId()).isEqualTo(42L);
        assertThat(captured.get().getSessionId()).isEqualTo(7L);
        assertThat(captured.get().getUserName()).isNull();
        assertThat(captured.get().getAccessTokenExpiresAt()).isEqualTo(expiresAt);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(Object::toString)
                .containsExactly("IDENTITY_USER_READ");
        verify(authenticationService).validate(42L, 7L);
        verify(authoritiesProvider).findAuthorityCodesByUserId(42L);
        verifyNoMoreInteractions(authenticationService, authoritiesProvider);
    }

    @Test
    void keepsRejectingLegacyTokenWhenItsSessionIsInvalid() throws Exception {
        JwtTokenParser parser = mock(JwtTokenParser.class);
        UserAuthoritiesProvider authoritiesProvider = mock(UserAuthoritiesProvider.class);
        AccessTokenAuthenticationService authenticationService = mock(AccessTokenAuthenticationService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<ApiProblemResponseWriter> problemWriterProvider = mock(ObjectProvider.class);
        when(parser.parseAndValidate("legacy-access-token")).thenReturn(
                new JwtAuthenticatedUser(
                        42L, 7L, null, Instant.parse("2026-09-13T12:00:00Z")
                )
        );
        doThrow(new SessionInvalidException("revoked"))
                .when(authenticationService).validate(42L, 7L);
        var filter = new JwtAuthenticationFilter(
                parser,
                authoritiesProvider,
                authenticationService,
                problemWriterProvider
        );
        var request = new MockHttpServletRequest("GET", "/identity/me");
        request.addHeader("Authorization", "Bearer legacy-access-token");
        var response = new MockHttpServletResponse();
        var chainInvoked = new AtomicBoolean();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chainInvoked).isFalse();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(authenticationService).validate(42L, 7L);
        org.mockito.Mockito.verifyNoInteractions(authoritiesProvider);
    }
}
