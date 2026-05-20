package com.jeepclub.backend.authentication.infra.persistence.repositoryJpa;

import com.jeepclub.backend.authentication.core.domain.model.PasswordRecoveryRequest;
import com.jeepclub.backend.authentication.core.repository.PasswordRecoveryRequestRepository;
import com.jeepclub.backend.authentication.infra.persistence.jpa.PasswordRecoveryRequestJpaRepository;
import com.jeepclub.backend.authentication.infra.persistence.mapper.PasswordRecoveryRequestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PasswordRecoveryRequestRepositoryJpa implements PasswordRecoveryRequestRepository {

    private final PasswordRecoveryRequestJpaRepository jpaRepository;

    @Override
    public PasswordRecoveryRequest save(PasswordRecoveryRequest request) {
        return PasswordRecoveryRequestMapper.toDomain(
                jpaRepository.save(PasswordRecoveryRequestMapper.toEntity(request))
        );
    }

    @Override
    public Optional<PasswordRecoveryRequest> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash)
                .map(PasswordRecoveryRequestMapper::toDomain);
    }
}