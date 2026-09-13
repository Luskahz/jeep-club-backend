package com.jeepclub.backend.platform.logging;

import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SystemRequestLoggingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUEST_ID_LENGTH = 100;
    private static final int MAX_PATH_LENGTH = 500;
    private static final int MAX_ACTION_LENGTH = 120;
    private final Optional<SystemLogService> systemLogService;
    private final ClientPlatformResolver clientPlatformResolver;

    public SystemRequestLoggingFilter(
            Optional<SystemLogService> systemLogService,
            ClientPlatformResolver clientPlatformResolver
    ) {
        this.systemLogService = systemLogService;
        this.clientPlatformResolver = clientPlatformResolver;
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

        try {
            filterChain.doFilter(request, response);
        } finally {
            restoreIdentityContext(request);
            int status = response.getStatus();
            SystemLogOutcome outcome = status >= 500
                    ? SystemLogOutcome.SERVER_ERROR
                    : status >= 400 ? SystemLogOutcome.CLIENT_ERROR : SystemLogOutcome.SUCCESS;
            String route = route(request);
            try {
                systemLogService.ifPresent(service -> service.record(new SystemLogEvent(
                        currentActorId(request),
                        limit(request.getMethod() + " " + route, MAX_ACTION_LENGTH),
                        request.getMethod(),
                        limit(request.getRequestURI(), MAX_PATH_LENGTH),
                        status,
                        outcome,
                        (System.nanoTime() - startedAt) / 1_000_000,
                        requestId,
                        Instant.now()
                )));
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

    private Long currentActorId(HttpServletRequest request) {
        Object enrichedUserId = request.getAttribute(HttpLoggingContext.USER_ID_ATTRIBUTE);
        if (enrichedUserId instanceof String value) {
            try {
                return Long.valueOf(value);
            } catch (NumberFormatException ignored) {
                // Fall through to the SecurityContext compatibility path.
            }
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUserId();
        }
        return null;
    }
}
