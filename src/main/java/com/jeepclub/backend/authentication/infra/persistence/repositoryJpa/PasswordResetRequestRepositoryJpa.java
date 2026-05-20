package com.jeepclub.backend.authentication.infra.persistence.repositoryJpa;

import com.jeepclub.backend.authentication.core.domain.model.PasswordResetRequest;
import com.jeepclub.backend.authentication.core.repository.PasswordResetRequestRepository;
import com.jeepclub.backend.authentication.infra.persistence.jpa.PasswordResetRequestJpaRepository;
import com.jeepclub.backend.authentication.infra.persistence.mapper.PasswordResetRequestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PasswordResetRequestRepositoryJpa implements PasswordResetRequestRepository {

    private final PasswordResetRequestJpaRepository jpaRepository;

    @Override
    public PasswordResetRequest save(PasswordResetRequest request) {
        return PasswordResetRequestMapper.toDomain(
                jpaRepository.save(PasswordResetRequestMapper.toEntity(request))
        );
    }

    @Override
    public Optional<PasswordResetRequest> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash)
                .map(PasswordResetRequestMapper::toDomain);
    }
}