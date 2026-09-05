package com.jeepclub.backend.iam.identity.core.application.service.user;

import com.jeepclub.backend.iam.identity.api.module.UserAdministration;
import com.jeepclub.backend.iam.identity.api.module.exception.UserNotFoundException;
import com.jeepclub.backend.iam.identity.core.application.query.user.AdminUserField;
import com.jeepclub.backend.iam.identity.core.application.query.user.AdminUserFilter;
import com.jeepclub.backend.iam.identity.core.application.result.admin.user.AdminUserResult;
import com.jeepclub.backend.iam.identity.core.repository.AdminUserQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminUserService {
    private final UserAdministration userAdministration;
    private final AdminUserQueryRepository adminUserQueryRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Page<AdminUserResult> findAll(
            AdminUserFilter filter,
            Set<AdminUserField> fields,
            Pageable pageable
    ) {
        return adminUserQueryRepository.findAll(filter, fields, pageable);
    }

    @Transactional(readOnly = true)
    public AdminUserResult findById(Long userId) {
        return adminUserQueryRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Transactional
    public AdminUserResult disable(Long userId) {
        userAdministration.disable(userId, Instant.now(clock));
        return findById(userId);
    }

    @Transactional
    public AdminUserResult enable(Long userId) {
        userAdministration.enable(userId, Instant.now(clock));
        return findById(userId);
    }
}
