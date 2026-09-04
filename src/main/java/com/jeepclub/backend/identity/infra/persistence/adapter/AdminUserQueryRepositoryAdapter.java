package com.jeepclub.backend.identity.infra.persistence.adapter;

import com.jeepclub.backend.identity.core.application.query.user.AdminUserField;
import com.jeepclub.backend.identity.core.application.query.user.AdminUserFilter;
import com.jeepclub.backend.identity.core.application.result.admin.user.AdminUserResult;
import com.jeepclub.backend.identity.core.repository.AdminUserQueryRepository;
import com.jeepclub.backend.identity.infra.persistence.query.AdminUserJpaQueryRepository;
import com.jeepclub.backend.identity.infra.persistence.sort.UserSortMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class AdminUserQueryRepositoryAdapter implements AdminUserQueryRepository {
    private final AdminUserJpaQueryRepository jpaQueryRepository;

    @Override
    public Page<AdminUserResult> findAll(
            AdminUserFilter filter,
            Set<AdminUserField> fields,
            Pageable pageable
    ) {
        return jpaQueryRepository.findAll(filter, fields, UserSortMapper.map(pageable));
    }

    @Override
    public Optional<AdminUserResult> findById(Long userId) {
        return jpaQueryRepository.findById(userId);
    }
}
