package com.jeepclub.backend.iam.identity.infra.persistence.adapter;

import com.jeepclub.backend.iam.identity.core.application.query.user.AdminUserField;
import com.jeepclub.backend.iam.identity.core.application.query.user.AdminUserFilter;
import com.jeepclub.backend.iam.identity.core.application.result.admin.user.AdminUserResult;
import com.jeepclub.backend.iam.identity.core.repository.AdminUserQueryRepository;
import com.jeepclub.backend.iam.identity.infra.persistence.query.AdminUserJpaQueryRepository;
import com.jeepclub.backend.iam.identity.infra.persistence.sort.UserSortMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AdminUserQueryRepositoryAdapter implements AdminUserQueryRepository {
    private final AdminUserJpaQueryRepository jpaQueryRepository;
    private final com.jeepclub.backend.iam.identity.api.module.spi.UserMembershipNumberProjection membershipNumbers;

    @Override
    public Page<AdminUserResult> findAll(
            AdminUserFilter filter,
            Set<AdminUserField> fields,
            Pageable pageable
    ) {
        Long exactId = filter.membershipNumber() == null ? null : membershipNumbers.identityIdByCurrentNumber(filter.membershipNumber()).orElse(0L);
        Long queryId = filter.query() == null ? null : membershipNumbers.identityIdByCurrentNumber(filter.query()).orElse(null);
        return jpaQueryRepository.findAll(filter.resolvedMembership(exactId,queryId), fields, UserSortMapper.map(pageable));
    }

    @Override
    public Optional<AdminUserResult> findById(Long userId) {
        return jpaQueryRepository.findById(userId);
    }
}
