package com.jeepclub.backend.authentication.core.application.service.user;

import com.jeepclub.backend.authentication.api.http.dto.admin.user.AdminUserFilterDTO;
import com.jeepclub.backend.authentication.core.application.exceptions.user.UserIdNotFoundException;
import com.jeepclub.backend.authentication.core.application.query.user.AdminUserField;
import com.jeepclub.backend.authentication.core.application.query.user.AdminUserFilter;
import com.jeepclub.backend.authentication.core.application.result.admin.user.AdminUserResult;
import com.jeepclub.backend.authentication.core.application.service.internal.CredentialRevocationService;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.repository.UserRepository;
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

    private final UserRepository userRepository;
    private final CredentialRevocationService credentialRevocationService;
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
    } public Page<AdminUserResult> findAll(
            AdminUserFilter filter,
            Pageable pageable
    ) {
        return userRepository
                .findAll(filter, pageable)
                .map(AdminUserResult::from);
    }

    @Transactional(readOnly = true)
    public AdminUserResult findById(Long userId) {
        User user = findUserById(userId);

        return AdminUserResult.from(user);
    }

    @Transactional
    public AdminUserResult disable(Long userId) {
        User user =
                findUserByIdForUpdate(userId);

        Instant now = Instant.now(clock);

        user.disable(now);
        credentialRevocationService.revokeAllForUser(user.getId(), now);

        User savedUser =
                userRepository.save(user);

        return AdminUserResult.from(savedUser);
    }

    @Transactional
    public AdminUserResult enable(Long userId) {
        User user =
                findUserByIdForUpdate(userId);

        Instant now = Instant.now(clock);

        user.enable(now);

        User savedUser =
                userRepository.save(user);

        return AdminUserResult.from(savedUser);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(
                        () -> new UserIdNotFoundException(
                                userId
                        )
                );
    }

    private User findUserByIdForUpdate(
            Long userId
    ) {
        return userRepository
                .findByIdForUpdate(userId)
                .orElseThrow(
                        () -> new UserIdNotFoundException(
                                userId
                        )
                );
    }
}
