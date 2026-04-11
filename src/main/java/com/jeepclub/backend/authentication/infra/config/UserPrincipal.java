package com.jeepclub.backend.authentication.infra.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.time.Instant;
import java.util.Collection;

public class UserPrincipal implements UserDetails {

    private final Long userId;
    private final String sessionId;
    private final Instant accessTokenExpiresAt;
    private final String username;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Long userId, String username, String password,
                         Collection<? extends GrantedAuthority> authorities,
                         String sessionId, Instant accessTokenExpiresAt) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.authorities = authorities;
        this.sessionId = sessionId;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
    }

    public Long getUserId() { return userId; }
    public String getSessionId() { return sessionId; }
    public Instant getAccessTokenExpiresAt() { return accessTokenExpiresAt; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

    @Override
    public String getPassword() { return password; }

    @Override
    public String getUsername() { return username; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}