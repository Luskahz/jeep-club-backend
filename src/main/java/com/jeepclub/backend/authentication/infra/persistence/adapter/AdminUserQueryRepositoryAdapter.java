package com.jeepclub.backend.authentication.infra.persistence.adapter;

import com.jeepclub.backend.authentication.core.application.query.user.AdminUserField;
import com.jeepclub.backend.authentication.core.application.query.user.AdminUserFilter;
import com.jeepclub.backend.authentication.core.application.result.admin.user.AdminUserResult;
import com.jeepclub.backend.authentication.core.repository.AdminUserQueryRepository;
import com.jeepclub.backend.authentication.infra.persistence.query.AdminUserJpaQueryRepository;
import com.jeepclub.backend.authentication.infra.persistence.sort.UserSortMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
@RequiredArgsConstructor
public class AdminUserQueryRepositoryAdapter
        implements AdminUserQueryRepository {

    private final AdminUserJpaQueryRepository jpaQueryRepository;

    @Override
    public Page<AdminUserResult> findAll(
            AdminUserFilter filter,
            Set<AdminUserField> fields,
            Pageable pageable
    ) {
        Pageable mappedPageable =
                UserSortMapper.map(pageable);

        return jpaQueryRepository.findAll(
                filter,
                fields,
                mappedPageable
        );
    }
}