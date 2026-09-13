package com.jeepclub.backend.platform.logging;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.stream.Stream;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

class SystemRequestLoggingFilterTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void recordsNormalizedRouteWithoutQueryParametersOrPersistentSystemLogDependency() throws Exception {
        HttpRequestLogWriter writer = mock(HttpRequestLogWriter.class);
        var filter = new SystemRequestLoggingFilter(new ClientPlatformResolver(), writer);
        var request = new MockHttpServletRequest("GET", "/vehicles/42");
        request.setQueryString("token=secret");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/vehicles/{vehicleId}");
        request.addHeader("X-Request-Id", "request-123");
        request.addHeader(ClientPlatformResolver.HEADER_NAME, "android");
        var response = new MockHttpServletResponse();
        var mdcAtCompletion = new AtomicReference<Map<String, String>>();
        doAnswer(invocation -> {
            mdcAtCompletion.set(MDC.getCopyOfContextMap());
            return null;
        }).when(writer).write(any());

        filter.doFilter(request, response, new MockFilterChain());

        var captor = org.mockito.ArgumentCaptor.forClass(HttpRequestLogEvent.class);
        verify(writer).write(captor.capture());
        assertThat(captor.getValue().path()).isEqualTo("/vehicles/{vehicleId}");
        assertThat(mdcAtCompletion.get())
                .containsEntry("requestId", "request-123")
                .containsEntry("device", "ANDROID")
                .doesNotContainKeys("userId", "userName");
        assertThat(response.getHeader("X-Request-Id")).isEqualTo("request-123");
        assertThat(request.getAttribute(ClientPlatformResolver.REQUEST_ATTRIBUTE))
                .isEqualTo(ClientPlatform.ANDROID);
    }

    @Test
    void loggingFailureNeverChangesBusinessResponse() throws Exception {
        HttpRequestLogWriter writer = mock(HttpRequestLogWriter.class);
        doThrow(new IllegalStateException("logging unavailable")).when(writer).write(any());
        var filter = new SystemRequestLoggingFilter(new ClientPlatformResolver(), writer);
        var request = new MockHttpServletRequest("POST", "/tools");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) ->
                ((jakarta.servlet.http.HttpServletResponse) res).setStatus(201));

        assertThat(response.getStatus()).isEqualTo(201);
    }

    @Test
    void includesEnrichedAuthenticatedIdentityInOperationalEvent() throws Exception {
        HttpRequestLogWriter writer = mock(HttpRequestLogWriter.class);
        var filter = new SystemRequestLoggingFilter(new ClientPlatformResolver(), writer);
        var request = new MockHttpServletRequest("GET", "/identity/me");
        request.setAttribute(HttpLoggingContext.USER_ID_ATTRIBUTE, "42");
        request.setAttribute(HttpLoggingContext.USER_NAME_ATTRIBUTE, "Lucas Alves");
        var mdcAtCompletion = new AtomicReference<Map<String, String>>();
        doAnswer(invocation -> {
            mdcAtCompletion.set(MDC.getCopyOfContextMap());
            return null;
        }).when(writer).write(any());

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        verify(writer).write(any());
        assertThat(mdcAtCompletion.get())
                .containsEntry("userId", "42")
                .containsEntry("userName", "Lucas Alves");
    }

    @Test
    void ignoresTechnicalRoutes() throws Exception {
        HttpRequestLogWriter writer = mock(HttpRequestLogWriter.class);
        var filter = new SystemRequestLoggingFilter(new ClientPlatformResolver(), writer);
        var request = new MockHttpServletRequest("GET", "/actuator/health");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        org.mockito.Mockito.verifyNoInteractions(writer);
    }

    @Test
    void exposesRequestIdAndDeviceInMdcDuringRequestAndAlwaysCleansTheThread() throws Exception {
        var filter = new SystemRequestLoggingFilter(new ClientPlatformResolver(), mock(HttpRequestLogWriter.class));
        var request = new MockHttpServletRequest("GET", "/vehicles");
        request.addHeader("X-Request-Id", "safe.request-123");
        request.addHeader(ClientPlatformResolver.HEADER_NAME, "WEB");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            assertThat(MDC.get(HttpLoggingContext.REQUEST_ID)).isEqualTo("safe.request-123");
            assertThat(MDC.get(HttpLoggingContext.DEVICE)).isEqualTo("WEB");
        });

        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    void replacesUnsafeRequestIdAndEchoesBackendGeneratedValue() throws Exception {
        var filter = new SystemRequestLoggingFilter(new ClientPlatformResolver(), mock(HttpRequestLogWriter.class));
        var request = new MockHttpServletRequest("GET", "/vehicles");
        request.addHeader("X-Request-Id", "forged\r\nrequest");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("X-Request-Id"))
                .isNotEqualTo("forged\r\nrequest")
                .matches("[0-9a-f-]{36}");
    }

    @Test
    void generatesAndEchoesRequestIdWhenHeaderIsMissing() throws Exception {
        var filter = new SystemRequestLoggingFilter(new ClientPlatformResolver(), mock(HttpRequestLogWriter.class));
        var response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("GET", "/vehicles"), response, new MockFilterChain());

        assertThat(response.getHeader("X-Request-Id")).matches("[0-9a-f-]{36}");
    }

    @ParameterizedTest
    @MethodSource("invalidRequestIds")
    void replacesEveryMalformedOrExcessiveRequestId(String supplied) throws Exception {
        var filter = new SystemRequestLoggingFilter(new ClientPlatformResolver(), mock(HttpRequestLogWriter.class));
        var request = new MockHttpServletRequest("GET", "/vehicles");
        request.addHeader("X-Request-Id", supplied);
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("X-Request-Id"))
                .isNotEqualTo(supplied)
                .matches("[0-9a-f-]{36}");
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 500})
    void cleansMdcAfterFourAndFiveHundredResponses(int status) throws Exception {
        var filter = new SystemRequestLoggingFilter(new ClientPlatformResolver(), mock(HttpRequestLogWriter.class));
        filter.doFilter(
                new MockHttpServletRequest("GET", "/failure"),
                new MockHttpServletResponse(),
                (req, res) -> ((jakarta.servlet.http.HttpServletResponse) res).setStatus(status)
        );

        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    void neverCopiesCredentialsPayloadQueryOrFullUserAgentIntoOperationalEvent() throws Exception {
        HttpRequestLogWriter writer = mock(HttpRequestLogWriter.class);
        var filter = new SystemRequestLoggingFilter(new ClientPlatformResolver(), writer);
        var request = new MockHttpServletRequest("POST", "/authentication/login");
        request.setQueryString("activationToken=query-secret");
        request.addHeader("Authorization", "Bearer jwt-secret");
        request.addHeader("Cookie", "refreshToken=cookie-secret");
        request.addHeader("User-Agent", "full-user-agent-secret");
        request.setContent("{\"password\":\"body-secret\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        var captor = org.mockito.ArgumentCaptor.forClass(HttpRequestLogEvent.class);
        verify(writer).write(captor.capture());
        String observableFields = captor.getValue().toString();
        assertThat(observableFields).doesNotContain(
                "query-secret", "jwt-secret", "cookie-secret", "full-user-agent-secret", "body-secret"
        );
    }

    @Test
    void cleansMdcAfterUnhandledException() {
        HttpRequestLogWriter writer = mock(HttpRequestLogWriter.class);
        var filter = new SystemRequestLoggingFilter(new ClientPlatformResolver(), writer);
        var request = new MockHttpServletRequest("GET", "/failure");
        var response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilter(request, response, (req, res) -> {
            throw new ServletException("boom");
        })).isInstanceOf(ServletException.class);

        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
        assertThat(response.getHeader("X-Request-Id")).matches("[0-9a-f-]{36}");
        var captor = org.mockito.ArgumentCaptor.forClass(HttpRequestLogEvent.class);
        verify(writer).write(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(500);
        assertThat(captor.getValue().unhandledFailure()).isTrue();
    }

    @Test
    void reusedThreadDoesNotLeakContextIntoNextRequest() throws Exception {
        var filter = new SystemRequestLoggingFilter(new ClientPlatformResolver(), mock(HttpRequestLogWriter.class));
        MDC.put(HttpLoggingContext.USER_ID, "stale-user");
        var first = new MockHttpServletRequest("GET", "/first");
        first.addHeader("X-Request-Id", "first-request");
        filter.doFilter(first, new MockHttpServletResponse(), (req, res) ->
                assertThat(MDC.get(HttpLoggingContext.USER_ID)).isNull());

        var second = new MockHttpServletRequest("GET", "/second");
        second.addHeader("X-Request-Id", "second-request");
        filter.doFilter(second, new MockHttpServletResponse(), (req, res) -> {
            assertThat(MDC.get(HttpLoggingContext.REQUEST_ID)).isEqualTo("second-request");
            assertThat(MDC.get(HttpLoggingContext.USER_ID)).isNull();
        });

        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    private static Stream<String> invalidRequestIds() {
        return Stream.of(
                " ",
                "contains spaces",
                "contains\tcontrol",
                "contains" + (char) 1 + "control",
                "a".repeat(101)
        );
    }
}
