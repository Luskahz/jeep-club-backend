package com.jeepclub.backend.iam.authorization.infra.security;

import com.jeepclub.backend.iam.authorization.core.application.query.UserPermissionQueryService;
import com.jeepclub.backend.platform.security.authorization.UserAuthoritiesProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthorizationUserAuthoritiesProvider implements UserAuthoritiesProvider {

    private final UserPermissionQueryService userPermissionQueryService;

    @Override
    public List<String> findAuthorityCodesByUserId(Long userId) {
        return userPermissionQueryService.findPermissionCodesByUserId(userId);
    }
}
