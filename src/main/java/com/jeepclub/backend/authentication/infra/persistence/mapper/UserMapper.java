package com.jeepclub.backend.authentication.infra.persistence.mapper;

import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.infra.persistence.entities.UserEntity;

/**
 * Utilitário responsável pela conversão bidirecional entre Camada de Infra (Entity) 
 * e Camada Core (Domain Model). Garante que a entidade do banco nunca suba para a regra
 * de negócio e vice-versa.
 */
public class UserMapper {

    private UserMapper() {
        // Classe utilitária
    }

    /**
     * Converte de JPA Entity para Modelo de Domínio
     * Utiliza o método estático 'reconstitute' do Core para manter a integridade sem alterar regras
     */
    public static User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return User.reconstitute(
                entity.getId(),
                entity.getName(),
                entity.getBirthData(),
                entity.getEmail(),
                entity.getCpf(),
                entity.getRg(),
                entity.getPasswordHash(),
                entity.getPhoneNumber(),
                entity.getProfilePhotoUrl(),
                entity.getStatus(),
                entity.getLastLoginAt(),
                entity.getCreatedAt(),
                entity.getDisabledAt(),
                entity.getUpdatedAt(),
                entity.getPasswordChangeAt(),
                entity.getFailedLoginAttempts()
        );
    }

    /**
     * Converte de Modelo de Domínio para JPA Entity
     */
    public static UserEntity toEntity(User domain) {
        if (domain == null) {
            return null;
        }

        UserEntity entity = new UserEntity();
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setBirthData(domain.getBirthData());
        entity.setEmail(domain.getEmail());
        entity.setCpf(domain.getCpf());
        entity.setRg(domain.getRg());
        entity.setPasswordHash(domain.getPasswordHash());
        entity.setPhoneNumber(domain.getPhoneNumber());
        entity.setProfilePhotoUrl(domain.getProfilePhotoUrl());
        entity.setStatus(domain.getStatus());
        entity.setLastLoginAt(domain.getLastLoginAt());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setDisabledAt(domain.getDisabledAt());
        entity.setUpdatedAt(domain.getUpdatedAt());

        return entity;
    }
}
