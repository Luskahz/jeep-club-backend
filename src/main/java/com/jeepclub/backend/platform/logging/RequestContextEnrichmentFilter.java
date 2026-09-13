package com.jeepclub.backend.platform.logging;

import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RequestContextEnrichmentFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal)) {
            filterChain.doFilter(request, response);
            return;
        }

        String userId = principal.getUserId().toString();
        String userName = principal.getUserName().replaceAll("[\\p{Cntrl}]", "_");
        request.setAttribute(HttpLoggingContext.USER_ID_ATTRIBUTE, userId);
        request.setAttribute(HttpLoggingContext.USER_NAME_ATTRIBUTE, userName);
        MDC.put(HttpLoggingContext.USER_ID, userId);
        MDC.put(HttpLoggingContext.USER_NAME, userName);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(HttpLoggingContext.USER_ID);
            MDC.remove(HttpLoggingContext.USER_NAME);
        }
    }
}
