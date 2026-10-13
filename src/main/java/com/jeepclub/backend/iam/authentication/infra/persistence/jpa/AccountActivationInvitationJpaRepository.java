package com.jeepclub.backend.iam.authentication.infra.persistence.jpa;

import com.jeepclub.backend.iam.authentication.infra.persistence.entity.AccountActivationInvitationEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AccountActivationInvitationJpaRepository extends JpaRepository<AccountActivationInvitationEntity, Long> {
    @Query("select invitation.deliveryStatus from AccountActivationInvitationEntity invitation where invitation.identityId = :identityId")
    Optional<String> findDeliveryStatusById(Long identityId);

    @Modifying
    @Query("update AccountActivationInvitationEntity invitation set invitation.deliveryStatus = :status "
            + "where invitation.identityId = :userId and invitation.tokenHash = :tokenHash")
    int updateDeliveryStatusIfCurrent(Long userId, String tokenHash, String status);
    Optional<AccountActivationInvitationEntity> findByTokenHash(String tokenHash);

    @Query("select invitation.identityId from AccountActivationInvitationEntity invitation where invitation.tokenHash = :tokenHash")
    Optional<Long> findIdentityIdByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invitation from AccountActivationInvitationEntity invitation where invitation.tokenHash = :tokenHash")
    Optional<AccountActivationInvitationEntity> findByTokenHashForUpdate(String tokenHash);
}
