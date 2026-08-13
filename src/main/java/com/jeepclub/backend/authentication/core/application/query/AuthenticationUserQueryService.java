package com.jeepclub.backend.authentication.core.application.query;

import com.jeepclub.backend.authentication.api.module.user.UserQuery;
import com.jeepclub.backend.authentication.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
class AuthenticationUserQueryService implements UserQuery {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long userId) {
        return userRepository.existsById(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findActiveUserIds() {
        return userRepository.findActiveUserIds();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsActiveUserById(Long userId) {
        Objects.requireNonNull(userId, "userId cannot be null");

        return userRepository.existsActiveById(userId);
    }

    @Override
    public boolean existsByCpf(String cpf) {
        return userRepository.existsByCpf(cpf);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}