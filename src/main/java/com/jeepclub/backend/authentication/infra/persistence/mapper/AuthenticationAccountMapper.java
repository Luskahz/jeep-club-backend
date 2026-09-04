package com.jeepclub.backend.authentication.infra.persistence.mapper;

import com.jeepclub.backend.authentication.core.domain.model.AuthenticationAccount;
import com.jeepclub.backend.authentication.infra.persistence.entity.AuthenticationAccountEntity;
import com.jeepclub.backend.identity.infra.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationAccountMapper {

    public AuthenticationAccountEntity toEntity(
            AuthenticationAccount domain,
            UserEntity user
    ) {
        if (domain == null) {
            return null;
        }

        AuthenticationAccountEntity entity = new AuthenticationAccountEntity();
        entity.setIdentityId(domain.getIdentityId());
        entity.setUser(user);
        entity.setPasswordHash(domain.getPasswordHash());
        entity.setAccessStatus(domain.getAccessStatus());
        entity.setAuthenticationStatus(domain.getAuthenticationStatus());
        entity.setCredentialStatus(domain.getCredentialStatus());
        entity.setLastLoginAt(domain.getLastLoginAt());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setAccessDisabledAt(domain.getAccessDisabledAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setPasswordChangedAt(domain.getPasswordChangedAt());
        entity.setFailedLoginAttempts(domain.getFailedLoginAttempts());
        return entity;
    }

    public AuthenticationAccount toDomain(AuthenticationAccountEntity entity) {
        if (entity == null) {
            return null;
        }

        return AuthenticationAccount.reconstitute(
                entity.getIdentityId(),
                entity.getPasswordHash(),
                entity.getAccessStatus(),
                entity.getAuthenticationStatus(),
                entity.getCredentialStatus(),
                entity.getLastLoginAt(),
                entity.getCreatedAt(),
                entity.getAccessDisabledAt(),
                entity.getUpdatedAt(),
                entity.getPasswordChangedAt(),
                entity.getFailedLoginAttempts()
        );
    }
}
