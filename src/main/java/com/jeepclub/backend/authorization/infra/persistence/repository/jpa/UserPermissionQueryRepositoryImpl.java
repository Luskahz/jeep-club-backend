package com.jeepclub.backend.authorization.infra.persistence.repository.jpa;

import com.jeepclub.backend.authorization.core.domain.enums.RoleStatus;
import com.jeepclub.backend.authorization.core.repository.UserPermissionQueryRepository;
import com.jeepclub.backend.authorization.infra.persistence.jpa.UserPermissionQueryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserPermissionQueryRepositoryImpl implements UserPermissionQueryRepository {

    private final UserPermissionQueryJpaRepository userPermissionQueryJpaRepository;

    @Override
    public List<String> findPermissionCodesByUserId(Long userId) {
        return userPermissionQueryJpaRepository
                .findPermissionCodesByUserId(userId, RoleStatus.ACTIVE)
                .stream()
                .map(Enum::name)
                .toList();
    }
}