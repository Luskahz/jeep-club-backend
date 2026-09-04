package com.jeepclub.backend.authorization.api.http.controller;

import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CurrentAuthorizationControllerTest {
    @Test
    void returnsUserIdAndSortedAuthoritiesOnly() {
        var principal = new UserPrincipal(42L, 7L, Instant.parse("2026-01-01T00:15:00Z"));
        var authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("Z_PERMISSION"), new SimpleGrantedAuthority("A_PERMISSION"))
        );

        var body = new CurrentAuthorizationController().getMe(principal, authentication).getBody();

        assertThat(body).isNotNull();
        assertThat(body.userId()).isEqualTo(42L);
        assertThat(body.authorities()).containsExactly("A_PERMISSION", "Z_PERMISSION");
    }
}
