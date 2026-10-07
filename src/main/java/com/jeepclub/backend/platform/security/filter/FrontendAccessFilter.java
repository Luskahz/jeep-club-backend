package com.jeepclub.backend.platform.security.filter;

import com.jeepclub.backend.platform.security.jwt.JwtProperties;
import com.jeepclub.backend.platform.web.exception.ApiProblemResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Collections;

/** Authenticates the frontend server before user JWT authentication. */
public class FrontendAccessFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Frontend-Key";
    private final boolean enabled;
    private final boolean documentationEnabled;
    private final byte[] expectedKey;
    private final ApiProblemResponseWriter problemWriter;

    public FrontendAccessFilter(
            boolean enabled,
            String secret,
            boolean documentationEnabled,
            JwtProperties jwtProperties,
            ApiProblemResponseWriter problemWriter) {
        this.enabled = enabled;
        this.documentationEnabled = documentationEnabled;
        this.problemWriter = problemWriter;
        this.expectedKey = (enabled
                ? (secret.isBlank() ? deriveKey(jwtProperties.getSecret()) : secret)
                : "").getBytes(StandardCharsets.UTF_8);
    }

    /** Purpose-separated HMAC; the derived credential never reveals the JWT signing key. */
    public static String deriveKey(String rootSecret) {
        if (rootSecret == null || rootSecret.isBlank()) {
            throw new IllegalStateException("A server secret is required for frontend access");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(rootSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    mac.doFinal("jeep-club/frontend-access/v1".getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot initialize frontend access authentication", e);
        }
    }

    public static boolean isInternalHealth(HttpServletRequest request) {
        return request.getLocalPort() == 8081
                && "GET".equals(request.getMethod())
                && "/actuator/health".equals(requestPath(request))
                && ("127.0.0.1".equals(request.getRemoteAddr()) || "::1".equals(request.getRemoteAddr()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = requestPath(request);
        if (!documentationEnabled && isDocumentation(path)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (enabled && !isInternalHealth(request)) {
            var supplied = Collections.list(request.getHeaders(HEADER));
            if (supplied.size() != 1 || !MessageDigest.isEqual(expectedKey,
                    supplied.get(0).getBytes(StandardCharsets.UTF_8))) {
                problemWriter.write(request, response, HttpStatus.FORBIDDEN, "ACCESS_DENIED");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private static boolean isDocumentation(String path) {
        return path.equals("/swagger-ui.html") || path.equals("/swagger-ui")
                || path.startsWith("/swagger-ui/") || path.equals("/v3/api-docs")
                || path.startsWith("/v3/api-docs/") || path.equals("/openapi-custom")
                || path.startsWith("/openapi-custom/") || path.equals("/docs") || path.startsWith("/docs/");
    }

    private static String requestPath(HttpServletRequest request) {
        return request.getServletPath().isEmpty()
                ? request.getRequestURI().substring(request.getContextPath().length())
                : request.getServletPath();
    }
}
