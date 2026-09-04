package com.jeepclub.backend.identity.core.application.service.user;

import com.jeepclub.backend.identity.api.module.UserDetails;
import com.jeepclub.backend.identity.api.module.UserQuery;
import com.jeepclub.backend.identity.core.domain.model.User;
import com.jeepclub.backend.identity.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
class UserQueryService implements UserQuery {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDetails> findById(Long userId) {
        return userRepository.findById(userId).map(UserQueryService::toDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDetails> findByCpf(String cpf) {
        return userRepository.findByCpf(cpf).map(UserQueryService::toDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long userId) {
        return userRepository.existsById(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findAdministrativelyActiveUserIds() {
        return userRepository.findActiveIds();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAdministrativelyActive(Long userId) {
        return userRepository.existsActiveById(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCpf(String cpf) {
        return cpf != null && userRepository.existsByCpf(User.normalizeCpf(cpf));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        String normalized = User.normalizeEmail(email);
        return normalized != null && userRepository.existsByEmail(normalized);
    }

    static UserDetails toDetails(User user) {
        return new UserDetails(
                user.getId(), user.getName(), user.getBirthDate(),
                user.getEmail(), user.getCpf(), user.getRg(),
                user.getPhoneNumber(), user.getProfilePhotoUrl(),
                user.isActive(), user.getCreatedAt(), user.getDisabledAt(),
                user.getUpdatedAt()
        );
    }
}
