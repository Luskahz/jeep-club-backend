package com.jeepclub.backend.authentication.core.application.services.admin;

import com.jeepclub.backend.authentication.core.application.exceptions.user.UserIdNotFoundException;
import com.jeepclub.backend.authentication.core.application.results.admin.user.AdminUserResult;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<AdminUserResult> findAll() {
        return AdminUserResult.from(userRepository.findAll());
    }

    @Transactional(readOnly = true)
    public AdminUserResult findById(Long userId) {
        User user = findUserById(userId);

        return AdminUserResult.from(user);
    }

    @Transactional
    public AdminUserResult disable(Long userId) {
        User user = findUserById(userId);
        Instant now = Instant.now(clock);

        user.disable(now);

        User savedUser = userRepository.save(user);

        return AdminUserResult.from(savedUser);
    }

    @Transactional
    public AdminUserResult enable(Long userId) {
        User user = findUserById(userId);
        Instant now = Instant.now(clock);

        user.enable(now);

        User savedUser = userRepository.save(user);

        return AdminUserResult.from(savedUser);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserIdNotFoundException(userId));
    }
}