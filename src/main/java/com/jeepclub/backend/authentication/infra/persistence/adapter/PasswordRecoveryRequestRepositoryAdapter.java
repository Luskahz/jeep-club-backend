package com.jeepclub.backend.authentication.infra.persistence.adapter;

import com.jeepclub.backend.authentication.core.domain.enums.PasswordRecoveryRequestMethod;
import com.jeepclub.backend.authentication.core.domain.enums.PasswordRecoveryRequestStatus;
import com.jeepclub.backend.authentication.core.domain.model.PasswordRecoveryRequest;
import com.jeepclub.backend.authentication.core.repository.PasswordRecoveryRequestRepository;
import com.jeepclub.backend.authentication.infra.persistence.jpa.PasswordRecoveryRequestJpaRepository;
import com.jeepclub.backend.authentication.infra.persistence.mapper.PasswordRecoveryRequestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PasswordRecoveryRequestRepositoryAdapter
        implements PasswordRecoveryRequestRepository {

    private final PasswordRecoveryRequestJpaRepository jpaRepository;

    @Override
    public PasswordRecoveryRequest save(
            PasswordRecoveryRequest request
    ) {
        return PasswordRecoveryRequestMapper.toDomain(
                jpaRepository.save(
                        PasswordRecoveryRequestMapper.toEntity(
                                request
                        )
                )
        );
    }

    @Override
    public Optional<PasswordRecoveryRequest> findByTokenHash(
            String tokenHash
    ) {
        return jpaRepository.findByTokenHash(tokenHash)
                .map(PasswordRecoveryRequestMapper::toDomain);
    }

    @Override
    public Optional<Long> findUserIdByTokenHash(
            String tokenHash
    ) {
        return jpaRepository.findUserIdByTokenHash(
                tokenHash
        );
    }

    @Override
    public Optional<PasswordRecoveryRequest>
    findByTokenHashForUpdate(
            String tokenHash
    ) {
        return jpaRepository
                .findByTokenHashForUpdate(tokenHash)
                .map(PasswordRecoveryRequestMapper::toDomain);
    }

    @Override
    public Optional<PasswordRecoveryRequest> findOpenByUserId(
            Long userId,
            Instant now
    ) {
        return jpaRepository
                .findFirstByUserIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                        userId,
                        PasswordRecoveryRequestStatus.OPEN,
                        now
                )
                .map(PasswordRecoveryRequestMapper::toDomain);
    }

    @Override
    public Optional<PasswordRecoveryRequest>
    findOpenByUserIdForUpdate(
            Long userId,
            Instant now
    ) {
        return jpaRepository
                .findTopByUserIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                        userId,
                        PasswordRecoveryRequestStatus.OPEN,
                        now
                )
                .map(PasswordRecoveryRequestMapper::toDomain);
    }

    @Override
    public Optional<PasswordRecoveryRequest>
    findOpenByUserIdAndMethod(
            Long userId,
            PasswordRecoveryRequestMethod method,
            Instant now
    ) {
        return jpaRepository
                .findFirstByUserIdAndStatusAndMethodAndExpiresAtAfterOrderByCreatedAtDesc(
                        userId,
                        PasswordRecoveryRequestStatus.OPEN,
                        method,
                        now
                )
                .map(PasswordRecoveryRequestMapper::toDomain);
    }

    @Override
    public Optional<PasswordRecoveryRequest>
    findOpenByUserIdAndMethodForUpdate(
            Long userId,
            PasswordRecoveryRequestMethod method,
            Instant now
    ) {
        return jpaRepository
                .findTopByUserIdAndStatusAndMethodAndExpiresAtAfterOrderByCreatedAtDesc(
                        userId,
                        PasswordRecoveryRequestStatus.OPEN,
                        method,
                        now
                )
                .map(PasswordRecoveryRequestMapper::toDomain);
    }

    @Override
    public List<PasswordRecoveryRequest> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(PasswordRecoveryRequestMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<PasswordRecoveryRequest> findById(
            Long id
    ) {
        return jpaRepository.findById(id)
                .map(PasswordRecoveryRequestMapper::toDomain);
    }

    @Override
    public Optional<PasswordRecoveryRequest> findByIdForUpdate(
            Long id
    ) {
        return jpaRepository.findByIdForUpdate(id)
                .map(PasswordRecoveryRequestMapper::toDomain);
    }

    @Override
    public List<PasswordRecoveryRequest> findByUserId(
            Long userId
    ) {
        return jpaRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(PasswordRecoveryRequestMapper::toDomain)
                .toList();
    }
}