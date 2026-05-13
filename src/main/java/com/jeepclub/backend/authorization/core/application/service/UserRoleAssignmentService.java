package com.jeepclub.backend.authorization.core.application.service;

import com.jeepclub.backend.authorization.core.domain.model.UserRole;
import com.jeepclub.backend.authorization.core.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserRoleAssignmentService {

    private final UserRoleRepository userRoleRepository;
    private final Clock clock;

    @Transactional
    public void assignRoleToUserIfMissing(
            Long userId,
            Long roleId
    ) {
        Instant now = Instant.now(clock);
        boolean alreadyAssigned = userRoleRepository.existsByUserIdAndRoleId(
                userId,
                roleId
        );

        if (alreadyAssigned) {
            return;
        }

        UserRole userRole = UserRole.create(userId, roleId, now);

        userRoleRepository.save(
                userRole
        );
    }
}