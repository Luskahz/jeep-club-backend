package com.jeepclub.backend.authentication.infra.persistence.adapter;

import com.jeepclub.backend.authentication.core.domain.model.PasswordResetRequest;
import com.jeepclub.backend.authentication.core.repository.PasswordResetRequestRepository;
import com.jeepclub.backend.authentication.infra.persistence.entity.PasswordResetRequestEntity;
import com.jeepclub.backend.authentication.infra.persistence.repository.JpaPasswordResetRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PasswordResetRequestRepositoryAdapter implements PasswordResetRequestRepository {

    private final JpaPasswordResetRequestRepository jpaRepository;

    @Override
    public PasswordResetRequest save(PasswordResetRequest request) {
        PasswordResetRequestEntity entity = new PasswordResetRequestEntity();
        if (request.getId() != null) {
            entity.setId(request.getId());
        }
        entity.setUserId(request.getUserId());
        entity.setTokenHash(request.getTokenHash());
        entity.setCreatedAt(request.getCreatedAt());
        entity.setExpiresAt(request.getExpiresAt());
        entity.setUsedAt(request.getUsedAt());
        entity.setStatus(request.getStatus());

        PasswordResetRequestEntity saved = jpaRepository.save(entity);

        return PasswordResetRequest.reconstitute(
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
    public Optional<PasswordResetRequest> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash)
                .map(entity -> PasswordResetRequest.reconstitute(
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
