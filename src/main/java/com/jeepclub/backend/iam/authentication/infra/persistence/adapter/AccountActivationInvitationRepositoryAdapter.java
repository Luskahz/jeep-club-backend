package com.jeepclub.backend.iam.authentication.infra.persistence.adapter;

import com.jeepclub.backend.iam.authentication.core.domain.model.AccountActivationInvitation;
import com.jeepclub.backend.iam.authentication.core.repository.AccountActivationInvitationRepository;
import com.jeepclub.backend.iam.authentication.infra.persistence.entity.AccountActivationInvitationEntity;
import com.jeepclub.backend.iam.authentication.infra.persistence.jpa.AccountActivationInvitationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AccountActivationInvitationRepositoryAdapter implements AccountActivationInvitationRepository {
    private final AccountActivationInvitationJpaRepository jpa;

    @Override
    public Optional<AccountActivationInvitation> findById(Long identityId) {
        return jpa.findById(identityId).map(this::toDomain);
    }

    @Override
    public Optional<String> findDeliveryStatusById(Long identityId) {
        return jpa.findDeliveryStatusById(identityId);
    }

    @Override
    public Optional<AccountActivationInvitation> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    public Optional<Long> findIdentityIdByTokenHash(String tokenHash) {
        return jpa.findIdentityIdByTokenHash(tokenHash);
    }

    @Override
    public Optional<AccountActivationInvitation> findByTokenHashForUpdate(String tokenHash) {
        return jpa.findByTokenHashForUpdate(tokenHash).map(this::toDomain);
    }

    @Override
    public void save(AccountActivationInvitation invitation) {
        AccountActivationInvitationEntity entity = jpa.findById(invitation.identityId())
                .orElseGet(AccountActivationInvitationEntity::new);
        entity.setIdentityId(invitation.identityId());
        entity.setTokenHash(invitation.tokenHash());
        entity.setExpiresAt(invitation.expiresAt());
        entity.setUsedAt(invitation.usedAt());
        entity.setDeliveryStatus(invitation.deliveryStatus());
        jpa.saveAndFlush(entity);
    }

    private AccountActivationInvitation toDomain(AccountActivationInvitationEntity entity) {
        return AccountActivationInvitation.reconstitute(entity.getIdentityId(), entity.getTokenHash(),
                entity.getExpiresAt(), entity.getUsedAt(), entity.getDeliveryStatus(), entity.getVersion());
    }
}
