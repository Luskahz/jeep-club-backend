package com.jeepclub.backend.platform.logging;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Optional;

class SystemRequestLoggingFilterTest {

    @Test
    void recordsNormalizedRouteAndDoesNotPersistQueryParameters() throws Exception {
        SystemLogService service = mock(SystemLogService.class);
        var filter = new SystemRequestLoggingFilter(Optional.of(service));
        var request = new MockHttpServletRequest("GET", "/vehicles/42");
        request.setQueryString("token=secret");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/vehicles/{vehicleId}");
        request.addHeader("X-Request-Id", "request-123");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        var captor = org.mockito.ArgumentCaptor.forClass(SystemLogEvent.class);
        verify(service).record(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo("GET /vehicles/{vehicleId}");
        assertThat(captor.getValue().path()).isEqualTo("/vehicles/42");
        assertThat(captor.getValue().requestId()).isEqualTo("request-123");
        assertThat(response.getHeader("X-Request-Id")).isEqualTo("request-123");
    }

    @Test
    void loggingFailureNeverChangesBusinessResponse() throws Exception {
        SystemLogService service = mock(SystemLogService.class);
        doThrow(new IllegalStateException("database unavailable")).when(service).record(any());
        var filter = new SystemRequestLoggingFilter(Optional.of(service));
        var request = new MockHttpServletRequest("POST", "/tools");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) ->
                ((jakarta.servlet.http.HttpServletResponse) res).setStatus(201));

        assertThat(response.getStatus()).isEqualTo(201);
    }

    @Test
    void ignoresTechnicalRoutes() throws Exception {
        SystemLogService service = mock(SystemLogService.class);
        var filter = new SystemRequestLoggingFilter(Optional.of(service));
        var request = new MockHttpServletRequest("GET", "/actuator/health");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        org.mockito.Mockito.verifyNoInteractions(service);
    }
}
