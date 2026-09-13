package com.jeepclub.backend.platform.logging;

import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
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
        request.setAttribute(
                ClientPlatformResolver.REQUEST_ATTRIBUTE,
                clientPlatformResolver.resolve(request)
        );
        response.setHeader("X-Request-Id", requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            int status = response.getStatus();
            SystemLogOutcome outcome = status >= 500
                    ? SystemLogOutcome.SERVER_ERROR
                    : status >= 400 ? SystemLogOutcome.CLIENT_ERROR : SystemLogOutcome.SUCCESS;
            String route = route(request);
            try {
                systemLogService.ifPresent(service -> service.record(new SystemLogEvent(
                        currentActorId(),
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
        if (supplied == null || supplied.isBlank() || supplied.length() > MAX_REQUEST_ID_LENGTH) {
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

    private Long currentActorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUserId();
        }
        return null;
    }
}
