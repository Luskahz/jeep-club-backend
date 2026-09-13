package com.jeepclub.backend.platform.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SystemRequestLoggingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUEST_ID_LENGTH = 100;
    private static final int MAX_PATH_LENGTH = 500;
    private final ClientPlatformResolver clientPlatformResolver;
    private final HttpRequestLogWriter logWriter;

    public SystemRequestLoggingFilter(
            ClientPlatformResolver clientPlatformResolver,
            HttpRequestLogWriter logWriter
    ) {
        this.clientPlatformResolver = clientPlatformResolver;
        this.logWriter = logWriter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long startedAt = System.nanoTime();
        String requestId = requestId(request);
        ClientPlatform device = clientPlatformResolver.resolve(request);
        MDC.clear();
        MDC.put(HttpLoggingContext.REQUEST_ID, requestId);
        MDC.put(HttpLoggingContext.DEVICE, device.name());
        request.setAttribute(HttpLoggingContext.REQUEST_ID_ATTRIBUTE, requestId);
        request.setAttribute(
                ClientPlatformResolver.REQUEST_ATTRIBUTE,
                device
        );
        response.setHeader("X-Request-Id", requestId);

        Throwable unhandledFailure = null;
        try {
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException | Error failure) {
            unhandledFailure = failure;
            throw failure;
        } finally {
            restoreIdentityContext(request);
            int status = unhandledFailure != null && response.getStatus() < 500
                    ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                    : response.getStatus();
            try {
                logWriter.write(new HttpRequestLogEvent(
                        requestId,
                        request.getMethod(),
                        singleLine(limit(route(request), MAX_PATH_LENGTH)),
                        status,
                        (System.nanoTime() - startedAt) / 1_000_000,
                        device.name(),
                        attributeOrPlaceholder(request, HttpLoggingContext.USER_ID_ATTRIBUTE),
                        attributeOrPlaceholder(request, HttpLoggingContext.USER_NAME_ATTRIBUTE),
                        unhandledFailure != null
                ));
            } catch (RuntimeException ignored) {
                // Logging must never change the result of the business request.
            } finally {
                MDC.clear();
            }
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/openapi-custom")
                || path.equals("/favicon.ico")
                || path.equals("/actuator/health");
    }

    private String requestId(HttpServletRequest request) {
        String supplied = request.getHeader("X-Request-Id");
        if (supplied == null
                || supplied.isBlank()
                || supplied.length() > MAX_REQUEST_ID_LENGTH
                || !supplied.matches("[A-Za-z0-9._:-]+")) {
            return UUID.randomUUID().toString();
        }
        return supplied;
    }

    private String route(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pattern instanceof String value ? value : request.getRequestURI();
    }

    private String limit(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String singleLine(String value) {
        return value.replaceAll("[\\p{Cntrl}]", "_");
    }

    private String attributeOrPlaceholder(HttpServletRequest request, String attributeName) {
        Object value = request.getAttribute(attributeName);
        return value instanceof String text && !text.isBlank() ? text : "-";
    }

    private void restoreIdentityContext(HttpServletRequest request) {
        Object userId = request.getAttribute(HttpLoggingContext.USER_ID_ATTRIBUTE);
        Object userName = request.getAttribute(HttpLoggingContext.USER_NAME_ATTRIBUTE);
        if (userId instanceof String value) {
            MDC.put(HttpLoggingContext.USER_ID, value);
        }
        if (userName instanceof String value) {
            MDC.put(HttpLoggingContext.USER_NAME, value);
        }
    }

}
