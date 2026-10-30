package com.jeepclub.backend.dependents.infra.integration.identity;

import com.jeepclub.backend.dependents.core.port.DependentPhotoAccess;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import com.jeepclub.backend.platform.security.authorization.UserAuthoritiesProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
class DependentPhotoAccessAdapter implements DependentPhotoAccess {
    private final UserQuery users;
    private final UserAuthoritiesProvider authorities;
    private final com.jeepclub.backend.dependents.core.port.DependentPhotoSessionAccess sessions;
    public boolean isActiveActor(Long actor) {
        if(!users.isAdministrativelyActive(actor))return false;
        var authentication=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if(authentication!=null && authentication.getPrincipal() instanceof com.jeepclub.backend.platform.security.principal.UserPrincipal principal) {
            if(!principal.getUserId().equals(actor) || !principal.getAccessTokenExpiresAt().isAfter(java.time.Instant.now(clock)))return false;
            sessions.validate(actor,principal.getSessionId());
        }
        return true;
    }
    private final java.time.Clock clock;
    public boolean canReadAsAdmin(Long actor) {
        return authorities.findAuthorityCodesByUserId(actor).contains("DEPENDENTS_DEPENDENT_READ");
    }
}
