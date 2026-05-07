package com.jeepclub.backend.infra.security.filter;

import com.jeepclub.backend.infra.security.authorization.UserAuthoritiesProvider;
import com.jeepclub.backend.infra.security.jwt.JwtAuthenticatedUser;
import com.jeepclub.backend.infra.security.jwt.JwtTokenParser;
import com.jeepclub.backend.infra.security.principal.UserPrincipal;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenParser tokenParser;
    private final UserAuthoritiesProvider userAuthoritiesProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            JwtAuthenticatedUser jwtUser =
                    tokenParser.parseAndValidate(token);

            List<SimpleGrantedAuthority> authorities =
                    userAuthoritiesProvider.findAuthorityCodesByUserId(jwtUser.userId())
                            .stream()
                            .distinct()
                            .map(SimpleGrantedAuthority::new)
                            .toList();

            UserPrincipal principal =
                    new UserPrincipal(
                            jwtUser.userId(),
                            jwtUser.sessionId(),
                            jwtUser.expiresAt()
                    );

            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            authorities
                    );

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

        } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }
}