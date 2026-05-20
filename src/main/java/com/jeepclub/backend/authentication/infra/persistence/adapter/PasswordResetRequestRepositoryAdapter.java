package com.jeepclub.backend.authentication.infra.persistence.adapter;

import com.jeepclub.backend.authentication.core.domain.model.PasswordRecoveryRequest;
import com.jeepclub.backend.authentication.core.repository.PasswordRecoveryRequestRepository;
import com.jeepclub.backend.authentication.infra.persistence.entity.PasswordRecoveryRequestEntity;
import com.jeepclub.backend.membershipKauan.infra.persistence.repository.JpaPasswordResetRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PasswordResetRequestRepositoryAdapter implements PasswordRecoveryRequestRepository {

    private final JpaPasswordResetRequestRepository jpaRepository;

    @Override
    public PasswordRecoveryRequest save(PasswordRecoveryRequest request) {
        PasswordRecoveryRequestEntity entity = new PasswordRecoveryRequestEntity();
        if (request.getId() != null) {
            entity.setId(request.getId());
        }
        entity.setUserId(request.getUserId());
        entity.setTokenHash(request.getTokenHash());
        entity.setCreatedAt(request.getCreatedAt());
        entity.setExpiresAt(request.getExpiresAt());
        entity.setUsedAt(request.getUsedAt());
        entity.setStatus(request.getStatus());

        PasswordRecoveryRequestEntity saved = jpaRepository.save(entity);

        return PasswordRecoveryRequest.reconstitute(
                saved.getId(),
                saved.getUserId(),
                saved.getTokenHash(),
                saved.getExpiresAt(),
                saved.getCreatedAt(),
                saved.getUsedAt(),
                saved.getStatus()
        );
    }

    @Override
    public Optional<PasswordRecoveryRequest> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash)
                .map(entity -> PasswordRecoveryRequest.reconstitute(
                        entity.getId(),
                        entity.getUserId(),
                        entity.getTokenHash(),
                        entity.getExpiresAt(),
                        entity.getCreatedAt(),
                        entity.getUsedAt(),
                        entity.getStatus()
                ));
    }
}
