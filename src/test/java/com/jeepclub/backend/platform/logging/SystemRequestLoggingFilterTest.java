package com.jeepclub.backend.platform.logging;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Optional;

class SystemRequestLoggingFilterTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void recordsNormalizedRouteAndDoesNotPersistQueryParameters() throws Exception {
        SystemLogService service = mock(SystemLogService.class);
        var filter = new SystemRequestLoggingFilter(Optional.of(service), new ClientPlatformResolver());
        var request = new MockHttpServletRequest("GET", "/vehicles/42");
        request.setQueryString("token=secret");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/vehicles/{vehicleId}");
        request.addHeader("X-Request-Id", "request-123");
        request.addHeader(ClientPlatformResolver.HEADER_NAME, "android");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        var captor = org.mockito.ArgumentCaptor.forClass(SystemLogEvent.class);
        verify(service).record(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo("GET /vehicles/{vehicleId}");
        assertThat(captor.getValue().path()).isEqualTo("/vehicles/42");
        assertThat(captor.getValue().requestId()).isEqualTo("request-123");
        assertThat(response.getHeader("X-Request-Id")).isEqualTo("request-123");
        assertThat(request.getAttribute(ClientPlatformResolver.REQUEST_ATTRIBUTE))
                .isEqualTo(ClientPlatform.ANDROID);
    }

    @Test
    void loggingFailureNeverChangesBusinessResponse() throws Exception {
        SystemLogService service = mock(SystemLogService.class);
        doThrow(new IllegalStateException("database unavailable")).when(service).record(any());
        var filter = new SystemRequestLoggingFilter(Optional.of(service), new ClientPlatformResolver());
        var request = new MockHttpServletRequest("POST", "/tools");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) ->
                ((jakarta.servlet.http.HttpServletResponse) res).setStatus(201));

        assertThat(response.getStatus()).isEqualTo(201);
    }

    @Test
    void ignoresTechnicalRoutes() throws Exception {
        SystemLogService service = mock(SystemLogService.class);
        var filter = new SystemRequestLoggingFilter(Optional.of(service), new ClientPlatformResolver());
        var request = new MockHttpServletRequest("GET", "/actuator/health");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        org.mockito.Mockito.verifyNoInteractions(service);
    }

    @Test
    void exposesRequestIdAndDeviceInMdcDuringRequestAndAlwaysCleansTheThread() throws Exception {
        var filter = new SystemRequestLoggingFilter(Optional.empty(), new ClientPlatformResolver());
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
        var filter = new SystemRequestLoggingFilter(Optional.empty(), new ClientPlatformResolver());
        var request = new MockHttpServletRequest("GET", "/vehicles");
        request.addHeader("X-Request-Id", "forged\r\nrequest");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("X-Request-Id"))
                .isNotEqualTo("forged\r\nrequest")
                .matches("[0-9a-f-]{36}");
    }

    @Test
    void cleansMdcAfterUnhandledException() {
        var filter = new SystemRequestLoggingFilter(Optional.empty(), new ClientPlatformResolver());
        var request = new MockHttpServletRequest("GET", "/failure");
        var response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilter(request, response, (req, res) -> {
            throw new ServletException("boom");
        })).isInstanceOf(ServletException.class);

        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
        assertThat(response.getHeader("X-Request-Id")).matches("[0-9a-f-]{36}");
    }

    @Test
    void reusedThreadDoesNotLeakContextIntoNextRequest() throws Exception {
        var filter = new SystemRequestLoggingFilter(Optional.empty(), new ClientPlatformResolver());
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
}
