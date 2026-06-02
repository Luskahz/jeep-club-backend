package com.jeepclub.backend.authentication.core.application.query;

import com.jeepclub.backend.authentication.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthenticationUserQueryService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<Long> findActiveUserIds() {
        return userRepository.findActiveUserIds();
    }

    @Transactional(readOnly = true)
    public boolean existsActiveUserById(Long userId) {
        Objects.requireNonNull(userId, "userId cannot be null");

        return userRepository.existsActiveById(userId);
    }
}