package com.jeepclub.backend.infra.security.principal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@AllArgsConstructor
public class UserPrincipal implements UserDetails {
    @Getter
    @NotNull private final Long userId;
    @Getter @NotNull private final Long sessionId;
    @Getter private final Instant accessTokenExpiresAt;

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }
    @Override public String getUsername() {
        return userId.toString();
    }
    @Override public @Nullable String getPassword() {
        return null;
    }
    @Override public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }
    @Override public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }
    @Override public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }
    @Override public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }


}
