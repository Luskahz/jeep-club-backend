package com.jeepclub.backend.iam.authorization.infra.persistence.adapter;

import com.jeepclub.backend.iam.authorization.core.domain.enums.RoleStatus;
import com.jeepclub.backend.iam.authorization.core.repository.UserPermissionQueryRepository;
import com.jeepclub.backend.iam.authorization.infra.persistence.jpa.UserPermissionQueryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserPermissionQueryRepositoryAdapter implements UserPermissionQueryRepository {

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
