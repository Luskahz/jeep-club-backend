package com.jeepclub.backend.authentication.core.application.service.user;

import com.jeepclub.backend.authentication.core.application.exceptions.user.UserIdNotFoundException;
import com.jeepclub.backend.authentication.core.application.query.user.AdminUserField;
import com.jeepclub.backend.authentication.core.application.query.user.AdminUserFilter;
import com.jeepclub.backend.authentication.core.application.result.admin.user.AdminUserResult;
import com.jeepclub.backend.authentication.core.repository.AdminUserQueryRepository;
import com.jeepclub.backend.identity.api.module.UserAdministration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserAdministration identityAdministration;
    private final AdminUserQueryRepository adminUserQueryRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Page<AdminUserResult> findAll(
            AdminUserFilter filter,
            Set<AdminUserField> fields,
            Pageable pageable
    ) {
        return adminUserQueryRepository.findAll(
                filter,
                fields,
                pageable
        );
    }

    @Transactional(readOnly = true)
    public AdminUserResult findById(Long userId) {
        return adminUserQueryRepository.findById(userId)
                .orElseThrow(() -> new UserIdNotFoundException(userId));
    }

    @Transactional
    public AdminUserResult disable(Long userId) {
        identityAdministration.disable(userId, Instant.now(clock));
        return findById(userId);
    }

    @Transactional
    public AdminUserResult enable(Long userId) {
        identityAdministration.enable(userId, Instant.now(clock));
        return findById(userId);
    }
}
