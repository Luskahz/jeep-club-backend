package com.jeepclub.backend.authorization.infra.security;

import com.jeepclub.backend.authorization.core.application.service.UserPermissionQueryService;
import com.jeepclub.backend.infra.security.authorization.UserAuthoritiesProvider;
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